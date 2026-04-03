package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.dto.pago.PedidoResponse;
import com.agroenvios.clientes.primary.model.PagoPendiente;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.model.PedidoItem;
import com.agroenvios.clientes.primary.repository.PagoPendienteRepository;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PagoPendienteRepository pagoPendienteRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Procesa el resultado de un pago de MercadoPago.
     * Si el pago fue aprobado, crea el pedido y marca el PagoPendiente como PROCESADO.
     * Idempotente: si ya fue procesado, no hace nada.
     */
    @Transactional
    public void procesarPago(String externalReference, String pagoId, String status) {
        if (externalReference == null || externalReference.isBlank()) {
            log.warn("Webhook recibido sin external_reference, pagoId={}", pagoId);
            return;
        }

        Optional<PagoPendiente> optPP = pagoPendienteRepository.findById(externalReference);
        if (optPP.isEmpty()) {
            log.warn("No se encontró PagoPendiente con referencia={}", externalReference);
            return;
        }

        PagoPendiente pp = optPP.get();

        if ("PROCESADO".equals(pp.getEstado())) {
            log.info("Pago referencia={} ya fue procesado (idempotencia), ignorando", externalReference);
            return;
        }

        switch (status) {
            case "approved" -> crearPedidoAprobado(pp, pagoId, externalReference);
            case "rejected", "cancelled" -> {
                pp.setEstado("RECHAZADO");
                pagoPendienteRepository.save(pp);
                log.info("Pago pagoId={} referencia={} fue {}", pagoId, externalReference, status);
            }
            default -> log.info("Pago pagoId={} referencia={} en estado intermedio: {}",
                    pagoId, externalReference, status);
        }
    }

    private void crearPedidoAprobado(PagoPendiente pp, String pagoId, String externalReference) {
        List<ItemPagoDto> items;
        try {
            items = objectMapper.readValue(pp.getItemsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ItemPagoDto.class));
        } catch (JsonProcessingException e) {
            log.error("Error al deserializar items de referencia={}", externalReference, e);
            return;
        }

        BigDecimal total = items.stream()
                .map(item -> BigDecimal.valueOf(item.getPrecio())
                        .multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = new Pedido();
        pedido.setUser(pp.getUser());
        pedido.setDireccionId(pp.getDireccionId());
        pedido.setTotal(total);
        pedido.setEstado("APROBADO");
        pedido.setPagoId(pagoId);
        pedido.setReferenciaPago(externalReference);

        List<PedidoItem> pedidoItems = items.stream().map(item -> {
            PedidoItem pi = new PedidoItem();
            pi.setPedido(pedido);
            pi.setNombre(item.getNombre());
            pi.setCantidad(item.getCantidad());
            pi.setPrecioUnitario(BigDecimal.valueOf(item.getPrecio()));
            return pi;
        }).toList();

        pedido.setItems(pedidoItems);
        pedidoRepository.save(pedido);

        pp.setEstado("PROCESADO");
        pagoPendienteRepository.save(pp);

        log.info("Pedido id={} creado para referencia={}", pedido.getId(), externalReference);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> getMisPedidos(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"))
                .getId();

        return pedidoRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse getPedidoById(Long pedidoId, String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"))
                .getId();

        Pedido pedido = pedidoRepository.findByIdAndUserId(pedidoId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        return PedidoResponse.from(pedido);
    }
}
