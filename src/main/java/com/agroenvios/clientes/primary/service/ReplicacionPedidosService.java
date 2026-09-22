package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.model.PedidoItem;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Red de seguridad del puente clientes → proveedores. El envío inicial
 * ({@link ExternalOrderBridgeService#bridgeToProveedores}) es de una sola oportunidad; si
 * falla (proveedores caído, un despliegue en curso, un error de datos…) el cliente ya pagó y
 * el comercio nunca se entera. Aquí se buscan los pedidos aprobados que proveedores no
 * confirmó tener y se reenvían, espaciando cada vez más los intentos.
 *
 * Reenviar es seguro: proveedores identifica el pedido por su referencia de pago y devuelve
 * el existente en vez de duplicarlo.
 *
 * Los fallos que agotan los intentos se marcan en el log con "[REPLICACION-AGOTADA]": esos ya
 * no se reintentan solos y hay que revisarlos a mano (el motivo queda en
 * pedidos.ultimo_error_replicacion).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplicacionPedidosService {

    /** Margen para que el envío inicial (asíncrono, justo al aprobarse el pago) termine antes de darlo por fallido. */
    static final Duration ESPERA_INICIAL = Duration.ofMinutes(3);

    /**
     * Solo se reintentan pedidos de este periodo. Más viejos ya requieren criterio humano
     * (el comercio pudo cancelar, el cliente pedir reembolso…) y reenviarlos solos sería
     * resucitar pedidos que quizá se dieron por perdidos.
     */
    static final Duration VENTANA = Duration.ofHours(72);

    private static final Duration ESPERA_BASE = Duration.ofMinutes(5);
    private static final Duration ESPERA_MAXIMA = Duration.ofHours(6);

    /** Tope de pedidos por pasada, para no dejar ocupado el hilo si de golpe hay muchos. */
    private static final int LOTE = 25;

    private final PedidoRepository pedidoRepository;
    private final ExternalOrderBridgeService bridge;

    @Scheduled(initialDelayString = "${proveedores.replicacion.retraso-inicial-ms:120000}",
            fixedDelayString = "${proveedores.replicacion.intervalo-ms:300000}")
    public void reintentarPendientes() {
        reintentarPendientes(LocalDateTime.now());
    }

    // Con la hora como parámetro para poder probar los tiempos de espera sin dormir el test.
    void reintentarPendientes(LocalDateTime ahora) {
        try {
            if (!bridge.estaConfigurado()) {
                return; // el propio envío ya avisa de la falta de configuración
            }

            List<Pedido> pendientes = pedidoRepository.findPendientesDeReplicar(
                            ahora.minus(VENTANA), ahora.minus(ESPERA_INICIAL), ExternalOrderBridgeService.MAX_INTENTOS)
                    .stream()
                    .filter(p -> yaLeToca(p, ahora))
                    .limit(LOTE)
                    .toList();

            if (pendientes.isEmpty()) {
                return;
            }

            log.warn("[REPLICACION] {} pedido(s) aprobado(s) sin confirmar en proveedores; reintentando",
                    pendientes.size());
            pendientes.forEach(this::reintentar);
        } catch (Exception e) {
            // Nada de esto debe tumbar el hilo del planificador ni las demás tareas programadas.
            log.error("[REPLICACION] Falló la pasada de reintentos: {}", e.getMessage(), e);
        }
    }

    private void reintentar(Pedido pedido) {
        try {
            User user = pedido.getUser();
            List<ItemPagoDto> items = pedido.getItems() == null
                    ? List.of()
                    : pedido.getItems().stream().map(ReplicacionPedidosService::aItemPago).toList();

            // Mismos datos que arma PedidoService.crearPedidoAprobado al aprobarse el pago.
            ExternalOrderBridgeService.Resultado resultado = bridge.replicar(pedido, items,
                    user.getCorreo(), (user.getNombre() + " " + user.getPaterno()).trim(), user.getTelefono(),
                    pedido.getCodigoEntrega());

            int envioNumero = pedido.getIntentosReplicacion() + 1;
            switch (resultado) {
                case REPLICADO -> log.info("[REPLICACION] Pedido id={} (referencia={}) ya está en proveedores",
                        pedido.getId(), pedido.getReferenciaPago());
                case ERROR -> {
                    if (envioNumero >= ExternalOrderBridgeService.MAX_INTENTOS) {
                        log.error("[REPLICACION-AGOTADA] Pedido id={} (referencia={}) sigue sin llegar a proveedores " +
                                        "tras {} intentos; ya no se reintenta solo, hay que revisarlo a mano " +
                                        "(pedidos.ultimo_error_replicacion)",
                                pedido.getId(), pedido.getReferenciaPago(), envioNumero);
                    } else {
                        log.warn("[REPLICACION] Pedido id={} sigue sin llegar a proveedores (intento {} de {})",
                                pedido.getId(), envioNumero, ExternalOrderBridgeService.MAX_INTENTOS);
                    }
                }
                case DESCARTADO -> log.warn("[REPLICACION] Pedido id={} no se puede replicar en proveedores; se descarta",
                        pedido.getId());
                case NO_CONFIGURADO -> { /* ya avisado por el puente */ }
            }
        } catch (Exception e) {
            // Un pedido problemático no debe impedir que se reintenten los demás.
            log.error("[REPLICACION] Error inesperado reintentando pedido id={}: {}", pedido.getId(), e.getMessage(), e);
        }
    }

    private boolean yaLeToca(Pedido pedido, LocalDateTime ahora) {
        LocalDateTime ultimo = pedido.getUltimoIntentoReplicacionAt();
        return ultimo == null || !ahora.isBefore(ultimo.plus(espera(pedido.getIntentosReplicacion())));
    }

    /** 5 min tras el primer fallo, luego el doble cada vez (10, 20, 40…) hasta un tope de 6 h. */
    static Duration espera(int intentosFallidos) {
        int duplicaciones = Math.min(Math.max(intentosFallidos - 1, 0), 10); // 2^10 basta para topar en 6 h
        Duration espera = ESPERA_BASE.multipliedBy(1L << duplicaciones);
        return espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera;
    }

    private static ItemPagoDto aItemPago(PedidoItem item) {
        ItemPagoDto dto = new ItemPagoDto();
        dto.setNombre(item.getNombre());
        dto.setCantidad(item.getCantidad());
        dto.setPrecio(item.getPrecioUnitario().doubleValue());
        dto.setProductId(item.getProductId());
        dto.setTradeShopId(item.getTradeShopId());
        return dto;
    }
}
