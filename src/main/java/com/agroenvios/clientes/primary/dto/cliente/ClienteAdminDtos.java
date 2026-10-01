package com.agroenvios.clientes.primary.dto.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Respuestas del directorio de clientes para el panel admin de proveedores (solo lectura). */
public final class ClienteAdminDtos {

    private ClienteAdminDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteListItem {
        private Long clienteId;
        private String username;
        private String nombre;
        private String correo;
        private String telefono;
        private Boolean correoVerificado;
        private Boolean telefonoVerificado;
        private Boolean activo;
        private LocalDateTime createdAt;
        private int totalPedidos;
        private int pedidosAprobados;
        // Suma de lo pagado en pedidos APROBADOS (dinero que realmente entró)
        private BigDecimal totalGastado;
        private LocalDateTime ultimoPedidoAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DireccionItem {
        private String nombre;
        private String calle;
        private String numeroExterior;
        private String numeroInterior;
        private String colonia;
        private String ciudad;
        private String estado;
        private Integer codigoPostal;
        private Boolean principal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidoItem {
        private Long pedidoId;
        private String estado;
        private String estadoEntrega;
        private BigDecimal total;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteDetail {
        private Long clienteId;
        private String username;
        private String nombre;
        private String paterno;
        private String materno;
        private String correo;
        private String telefono;
        private Boolean correoVerificado;
        private Boolean telefonoVerificado;
        private Boolean activo;
        private LocalDateTime createdAt;
        private int totalPedidos;
        private int pedidosAprobados;
        private BigDecimal totalGastado;
        private List<DireccionItem> direcciones;
        // Los más recientes primero (máx. 10)
        private List<PedidoItem> pedidosRecientes;
    }
}
