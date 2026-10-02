package com.agroenvios.clientes.primary.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Página de notificaciones para scroll infinito, con el total de sin leer de toda la cuenta. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageDTO {
    private List<NotificationDTO> items;
    private boolean hasMore;
    private int page;
    private long unreadCount;
}
