package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.cliente.ClienteAdminDtos.*;
import com.agroenvios.clientes.primary.model.DireccionEntrega;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.DireccionEntregaRepository;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Directorio de clientes para el panel admin de proveedores (solo lectura). */
@Service
@RequiredArgsConstructor
public class ClientesAdminService {

    private static final String APROBADO = "APROBADO";

    private final UserRepository userRepository;
    private final PedidoRepository pedidoRepository;
    private final DireccionEntregaRepository direccionEntregaRepository;

    @Transactional(readOnly = true)
    public List<ClienteListItem> listar() {
        // Un solo recorrido de pedidos agrupado por cliente, en vez de una consulta por cliente.
        Map<Long, List<Pedido>> pedidosPorCliente = pedidoRepository.findAllConUsuarioOrderByCreatedAtDesc().stream()
                .collect(Collectors.groupingBy(p -> p.getUser().getId()));

        return userRepository.findAll().stream()
                .map(u -> {
                    List<Pedido> pedidos = pedidosPorCliente.getOrDefault(u.getId(), List.of());
                    List<Pedido> aprobados = pedidos.stream().filter(p -> APROBADO.equals(p.getEstado())).toList();
                    return ClienteListItem.builder()
                            .clienteId(u.getId())
                            .username(u.getUsername())
                            .nombre(nombreCompleto(u))
                            .correo(u.getCorreo())
                            .telefono(u.getTelefono())
                            .correoVerificado(u.getIsEmailVerified())
                            .telefonoVerificado(u.getIsTelefonoVerified())
                            .activo(u.getIsActive())
                            .createdAt(u.getCreatedAt())
                            .totalPedidos(pedidos.size())
                            .pedidosAprobados(aprobados.size())
                            .totalGastado(suma(aprobados))
                            // la lista ya viene del más reciente al más antiguo
                            .ultimoPedidoAt(pedidos.isEmpty() ? null : pedidos.get(0).getCreatedAt())
                            .build();
                })
                .sorted(Comparator.comparing(ClienteListItem::getNombre, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ClienteDetail> detalle(Long clienteId) {
        return userRepository.findAll().stream()
                .filter(u -> clienteId.equals(u.getId()))
                .findFirst()
                .map(u -> {
                    List<Pedido> pedidos = pedidoRepository.findByUserIdOrderByCreatedAtDesc(u.getId());
                    List<Pedido> aprobados = pedidos.stream().filter(p -> APROBADO.equals(p.getEstado())).toList();
                    List<DireccionEntrega> direcciones = direccionEntregaRepository.findByUserId(u.getId());
                    return ClienteDetail.builder()
                            .clienteId(u.getId())
                            .username(u.getUsername())
                            .nombre(u.getNombre())
                            .paterno(u.getPaterno())
                            .materno(u.getMaterno())
                            .correo(u.getCorreo())
                            .telefono(u.getTelefono())
                            .correoVerificado(u.getIsEmailVerified())
                            .telefonoVerificado(u.getIsTelefonoVerified())
                            .activo(u.getIsActive())
                            .createdAt(u.getCreatedAt())
                            .totalPedidos(pedidos.size())
                            .pedidosAprobados(aprobados.size())
                            .totalGastado(suma(aprobados))
                            .direcciones(direcciones.stream()
                                    .map(d -> DireccionItem.builder()
                                            .nombre(d.getNombre())
                                            .calle(d.getCalle())
                                            .numeroExterior(d.getNumeroExterior())
                                            .numeroInterior(d.getNumeroInterior())
                                            .colonia(d.getColonia())
                                            .ciudad(d.getCiudad())
                                            .estado(d.getEstado())
                                            .codigoPostal(d.getCodigoPostal())
                                            .principal(d.getEsPrincipal())
                                            .build())
                                    .collect(Collectors.toList()))
                            .pedidosRecientes(pedidos.stream().limit(10)
                                    .map(p -> PedidoItem.builder()
                                            .pedidoId(p.getId())
                                            .estado(p.getEstado())
                                            .estadoEntrega(p.getEstadoEntrega())
                                            .total(p.getTotal())
                                            .createdAt(p.getCreatedAt())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build();
                });
    }

    private static BigDecimal suma(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String nombreCompleto(User u) {
        String nombre = String.join(" ",
                u.getNombre() != null ? u.getNombre() : "",
                u.getPaterno() != null ? u.getPaterno() : "",
                u.getMaterno() != null ? u.getMaterno() : "").trim().replaceAll("\\s+", " ");
        return nombre.isEmpty() ? u.getUsername() : nombre;
    }
}
