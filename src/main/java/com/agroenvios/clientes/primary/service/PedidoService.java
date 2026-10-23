package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.dto.pago.PedidoPageDTO;
import com.agroenvios.clientes.primary.dto.pago.PedidoResponse;
import com.agroenvios.clientes.primary.model.PagoPendiente;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.model.PedidoFacturaSolicitud;
import com.agroenvios.clientes.primary.model.PedidoItem;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.PagoPendienteRepository;
import com.agroenvios.clientes.primary.repository.PedidoFacturaSolicitudRepository;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import com.agroenvios.clientes.secondary.model.TradeShop;
import com.agroenvios.clientes.secondary.repository.TradeShopRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class PedidoService {

    private static final int MAX_PAGE_SIZE_PEDIDOS = 50;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PedidoRepository pedidoRepository;
    private final PagoPendienteRepository pagoPendienteRepository;
    private final UserRepository userRepository;
    private final TradeShopRepository tradeShopRepository;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;
    private final ExpoPushNotificationService pushNotificationService;
    private final ExternalOrderBridgeService externalOrderBridgeService;
    private final MercadoPagoService mercadoPagoService;
    private final EmailService emailService;
    private final PedidoFacturaSolicitudRepository facturaSolicitudRepository;

    @Value("${aws.s3.proveedores-bucket:agroenvios-files}")
    private String proveedoresBucket;

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
                User user = pp.getUser();
                pushNotificationService.sendPedidoNotification(user, user.getUsername(), user.getPushToken(), "RECHAZADO", null);
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

        BigDecimal subtotal = items.stream()
                .map(item -> BigDecimal.valueOf(item.getPrecio())
                        .multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tarifaEnvio = pp.getTarifaEnvio() != null ? pp.getTarifaEnvio() : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(tarifaEnvio);
        BigDecimal comisionMercadoPago = mercadoPagoService.obtenerComisionMercadoPago(pagoId, total);

        // Se extrae aquí, con la sesión de Hibernate todavía activa, porque más abajo se
        // pasa a métodos @Async (bridgeToProveedores, sendPedidoNotification) que corren en
        // otro hilo sin sesión: tocar el proxy lazy de User ahí revienta con "no session".
        User user = pp.getUser();

        Pedido pedido = new Pedido();
        pedido.setUser(user);
        pedido.setDireccionId(pp.getDireccionId());
        pedido.setSubtotal(subtotal);
        pedido.setTarifaEnvio(tarifaEnvio);
        pedido.setTotal(total);
        pedido.setComisionMercadoPago(comisionMercadoPago);
        pedido.setEstado("APROBADO");
        pedido.setPagoId(pagoId);
        pedido.setReferenciaPago(externalReference);
        pedido.setCodigoEntrega(String.format("%04d", new SecureRandom().nextInt(10000)));

        List<PedidoItem> pedidoItems = items.stream().map(item -> {
            PedidoItem pi = new PedidoItem();
            pi.setPedido(pedido);
            pi.setNombre(item.getNombre());
            pi.setCantidad(item.getCantidad());
            pi.setPrecioUnitario(BigDecimal.valueOf(item.getPrecio()));
            pi.setProductId(item.getProductId());
            pi.setTradeShopId(item.getTradeShopId());
            return pi;
        }).toList();

        pedido.setItems(pedidoItems);
        pedidoRepository.save(pedido);

        pp.setEstado("PROCESADO");
        pagoPendienteRepository.save(pp);

        log.info("Pedido id={} creado para referencia={}", pedido.getId(), externalReference);
        pushNotificationService.sendPedidoNotification(user, user.getUsername(), user.getPushToken(), "APROBADO", pedido.getId());

        // Replicar el pedido en el sistema de proveedores para que el comercio lo vea
        externalOrderBridgeService.bridgeToProveedores(pedido, items,
                user.getCorreo(), (user.getNombre() + " " + user.getPaterno()).trim(), user.getTelefono(),
                pedido.getCodigoEntrega());
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> getMisPedidos(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"))
                .getId();

        List<Pedido> pedidos = pedidoRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, PedidoResponse.TradeShopInfo> tradeShopById = resolveTradeShopInfo(pedidos);
        Map<Long, Map<Long, LocalDateTime>> facturas = resolverFacturas(pedidos);

        return pedidos.stream()
                .map(pedido -> PedidoResponse.from(pedido, tradeShopById, facturas.getOrDefault(pedido.getId(), Map.of())))
                .toList();
    }

    /**
     * Pedidos del cliente en páginas para scroll infinito, del más reciente al más antiguo.
     * {@code estado} es opcional y se filtra aquí (no en la app) para que las páginas y los
     * contadores de los chips sigan siendo correctos; los conteos por estado se calculan
     * solo para la página 0.
     */
    @Transactional(readOnly = true)
    public PedidoPageDTO getMisPedidosPaginado(String username, String estado, String busqueda, int page, int size) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"))
                .getId();

        int pagina = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(pagina, Math.min(Math.max(size, 1), MAX_PAGE_SIZE_PEDIDOS));
        String estadoFiltro = (estado == null || estado.isBlank()) ? null : estado.trim().toUpperCase();

        String texto = busqueda == null ? "" : busqueda.trim().replaceFirst("^#", "").toLowerCase();

        Slice<Pedido> slice;
        if (!texto.isEmpty()) {
            slice = pedidoRepository.buscarDelUsuario(userId, estadoFiltro, "%" + texto + "%", pageable);
        } else if (estadoFiltro == null) {
            slice = pedidoRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        } else {
            slice = pedidoRepository.findByUserIdAndEstadoOrderByCreatedAtDescIdDesc(userId, estadoFiltro, pageable);
        }

        List<Pedido> pedidos = slice.getContent();
        Map<Long, PedidoResponse.TradeShopInfo> tradeShopById = resolveTradeShopInfo(pedidos);
        Map<Long, Map<Long, LocalDateTime>> facturas = resolverFacturas(pedidos);

        return PedidoPageDTO.builder()
                .items(pedidos.stream().map(p -> PedidoResponse.from(p, tradeShopById, facturas.getOrDefault(p.getId(), Map.of()))).toList())
                .hasMore(slice.hasNext())
                .page(pagina)
                .conteos(pagina == 0 ? conteosPorEstado(userId) : null)
                .build();
    }

    private Map<String, Long> conteosPorEstado(Long userId) {
        Map<String, Long> conteos = new HashMap<>();
        for (Object[] fila : pedidoRepository.contarPorEstado(userId)) {
            conteos.put((String) fila[0], ((Number) fila[1]).longValue());
        }
        return conteos;
    }

    @Transactional(readOnly = true)
    public PedidoResponse getPedidoById(Long pedidoId, String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"))
                .getId();

        Pedido pedido = pedidoRepository.findByIdAndUserId(pedidoId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        return PedidoResponse.from(pedido, resolveTradeShopInfo(List.of(pedido)),
                resolverFacturas(List.of(pedido)).getOrDefault(pedido.getId(), Map.of()));
    }

    /**
     * El cliente pide su factura por correo a un comercio de su pedido (cada comercio factura
     * lo suyo): se le avisa con sus productos y el correo del cliente como Reply-To. Una vez
     * por comercio y hasta el último día del mes de la compra (el mismo criterio que usa la
     * app para mostrar el botón).
     */
    @Transactional
    public PedidoResponse solicitarFactura(Long pedidoId, Long tradeShopId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findByIdAndUserId(pedidoId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        List<PedidoItem> items = pedido.getItems().stream()
                .filter(item -> tradeShopId.equals(item.getTradeShopId()))
                .toList();
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ese comercio no es parte del pedido");
        }
        if (facturaSolicitudRepository.existsByPedidoIdAndTradeShopId(pedidoId, tradeShopId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya solicitaste la factura a este comercio");
        }
        if (!PedidoResponse.puedeSolicitarFactura(pedido)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La factura de este pedido ya no se puede solicitar (vencía el "
                            + PedidoResponse.limiteFactura(pedido) + ")");
        }

        String correo = tradeShopRepository.findCorreoContacto(tradeShopId).stream().findFirst()
                .orElseThrow(() -> {
                    log.warn("Pedido {}: comercio {} sin correo de contacto para solicitud de factura", pedidoId, tradeShopId);
                    return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "No pudimos localizar el correo del comercio. Intenta más tarde o contacta a soporte");
                });

        Map<Long, PedidoResponse.TradeShopInfo> tiendas = resolveTradeShopInfo(List.of(pedido));
        PedidoResponse.TradeShopInfo tienda = tiendas.get(tradeShopId);

        String nombreCliente = Stream.of(user.getNombre(), user.getPaterno(), user.getMaterno())
                .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
        BigDecimal total = items.stream()
                .map(i -> i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<EmailService.LineaFactura> lineas = items.stream()
                .map(i -> new EmailService.LineaFactura(
                        i.getNombre(),
                        BigDecimal.valueOf(i.getCantidad()).stripTrailingZeros().toPlainString(),
                        moneda(i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))))
                .toList();

        emailService.sendInvoiceRequestEmail(correo,
                tienda != null && tienda.nombre() != null ? tienda.nombre() : "Comercio",
                pedido.getId(), pedido.getCreatedAt().format(FORMATO_FECHA),
                nombreCliente, user.getCorreo(), user.getTelefono(),
                lineas, moneda(total), PedidoResponse.limiteFactura(pedido).format(FORMATO_FECHA));

        PedidoFacturaSolicitud solicitud = new PedidoFacturaSolicitud();
        solicitud.setPedidoId(pedidoId);
        solicitud.setTradeShopId(tradeShopId);
        solicitud.setSolicitadaAt(LocalDateTime.now());
        try {
            facturaSolicitudRepository.save(solicitud);
        } catch (DataIntegrityViolationException e) {
            // Doble toque en paralelo: la otra solicitud ya quedó registrada.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya solicitaste la factura a este comercio");
        }
        log.info("Pedido id={}: factura solicitada al comercio {}", pedidoId, tradeShopId);

        return PedidoResponse.from(pedido, tiendas, resolverFacturas(List.of(pedido)).getOrDefault(pedidoId, Map.of()));
    }

    // pedidoId -> (tradeShopId -> cuándo se pidió la factura), en una sola consulta.
    private Map<Long, Map<Long, LocalDateTime>> resolverFacturas(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            return Map.of();
        }
        return facturaSolicitudRepository.findByPedidoIdIn(pedidos.stream().map(Pedido::getId).toList()).stream()
                .collect(Collectors.groupingBy(PedidoFacturaSolicitud::getPedidoId,
                        Collectors.toMap(PedidoFacturaSolicitud::getTradeShopId,
                                PedidoFacturaSolicitud::getSolicitadaAt)));
    }

    private static String moneda(BigDecimal valor) {
        return "$" + String.format(Locale.US, "%,.2f", valor);
    }

    // Un solo findAllById para todos los tradeShopId distintos de todos los items,
    // en vez de una consulta por item — evita N+1 contra la BD de proveedores.
    private Map<Long, PedidoResponse.TradeShopInfo> resolveTradeShopInfo(List<Pedido> pedidos) {
        Set<Long> tradeShopIds = pedidos.stream()
                .flatMap(p -> p.getItems().stream())
                .map(PedidoItem::getTradeShopId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (tradeShopIds.isEmpty()) {
            return Map.of();
        }

        return tradeShopRepository.findAllById(tradeShopIds).stream()
                .collect(Collectors.toMap(TradeShop::getId, shop -> new PedidoResponse.TradeShopInfo(
                        shop.getNombreNegocio(),
                        minioService.generatePresignedUrl(shop.getImageKey(), proveedoresBucket))));
    }

    /**
     * Recibe el progreso del pedido desde proveedores (comercio confirmó, listo para
     * entrega, en camino, entregado, etc.) y lo guarda para que el cliente lo vea.
     * Silencioso si no encuentra el pedido: puede ser una orden de proveedores que
     * nunca se originó aquí (creada directamente ahí, o de prueba).
     */
    @Transactional
    public void actualizarEstadoEntrega(String referenciaPago, String status) {
        if (referenciaPago == null || referenciaPago.isBlank()) {
            return;
        }

        pedidoRepository.findByReferenciaPago(referenciaPago).ifPresentOrElse(pedido -> {
            pedido.setEstadoEntrega(status);
            pedidoRepository.save(pedido);
            log.info("Pedido id={} (referencia={}) actualizado a estadoEntrega={}",
                    pedido.getId(), referenciaPago, status);
            User user = pedido.getUser();
            pushNotificationService.sendPedidoNotification(user, user.getUsername(), user.getPushToken(), status, pedido.getId());
        }, () -> log.warn("No se encontró pedido con referencia={} para actualizar estadoEntrega={}",
                referenciaPago, status));
    }
}
