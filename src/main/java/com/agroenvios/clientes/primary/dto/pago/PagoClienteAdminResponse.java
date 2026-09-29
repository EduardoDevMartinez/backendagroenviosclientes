package com.agroenvios.clientes.primary.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un pago hecho por un cliente (un pedido), para el historial de pagos del panel admin. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoClienteAdminResponse {
    // Folio del pedido: el mismo que se muestra en todas las apps.
    private Long pedidoId;
    private Long clienteId;
    private String clienteNombre;
    private String clienteCorreo;
    // APROBADO, RECHAZADO, PENDIENTE, CANCELADO
    private String estado;
    private String estadoEntrega;
    private BigDecimal subtotal;
    private BigDecimal tarifaEnvio;
    private BigDecimal total;
    private BigDecimal comisionMercadoPago;
    // ID del pago en Mercado Pago; null si nunca llegó a generarse.
    private String pagoId;
    private String referenciaPago;
    private LocalDateTime createdAt;
}
