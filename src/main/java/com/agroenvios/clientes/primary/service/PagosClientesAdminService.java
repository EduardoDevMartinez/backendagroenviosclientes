package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.PagoClienteAdminResponse;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagosClientesAdminService {

    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public List<PagoClienteAdminResponse> listarPagos() {
        return pedidoRepository.findAllConUsuarioOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PagoClienteAdminResponse toResponse(Pedido p) {
        User u = p.getUser();
        return PagoClienteAdminResponse.builder()
                .pedidoId(p.getId())
                .clienteId(u.getId())
                .clienteNombre(nombreCompleto(u))
                .clienteCorreo(u.getCorreo())
                .estado(p.getEstado())
                .estadoEntrega(p.getEstadoEntrega())
                .subtotal(p.getSubtotal())
                .tarifaEnvio(p.getTarifaEnvio())
                .total(p.getTotal())
                .comisionMercadoPago(p.getComisionMercadoPago())
                .pagoId(p.getPagoId())
                .referenciaPago(p.getReferenciaPago())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private String nombreCompleto(User u) {
        String nombre = String.join(" ",
                u.getNombre() != null ? u.getNombre() : "",
                u.getPaterno() != null ? u.getPaterno() : "",
                u.getMaterno() != null ? u.getMaterno() : "").trim().replaceAll("\\s+", " ");
        return nombre.isEmpty() ? u.getUsername() : nombre;
    }
}
