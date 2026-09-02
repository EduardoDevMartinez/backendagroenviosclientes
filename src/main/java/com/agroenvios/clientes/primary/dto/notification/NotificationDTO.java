package com.agroenvios.clientes.primary.dto.notification;

import com.agroenvios.clientes.primary.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String estado;
    private String title;
    private String message;
    private Long pedidoId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationDTO from(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .estado(n.getEstado())
                .title(n.getTitle())
                .message(n.getMessage())
                .pedidoId(n.getPedidoId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
