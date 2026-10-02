package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.notification.NotificationDTO;
import com.agroenvios.clientes.primary.dto.notification.NotificationPageDTO;
import com.agroenvios.clientes.primary.model.Notification;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.NotificationRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import com.agroenvios.clientes.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * Persiste la notificación para la lista en la app. Se llama siempre que hay un
     * cambio de estado de pedido con copy definido, independientemente de si el
     * usuario tiene push token registrado (eso solo afecta si además le llega el push).
     * También la empuja por SSE (GET /notifications/stream) al usuario dueño.
     */
    @Transactional
    public void createNotification(User user, String estado, String title, String message, Long pedidoId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setEstado(estado);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPedidoId(pedidoId);
        notification.setIsRead(false);
        notification = notificationRepository.save(notification);

        sseEmitterRegistry.sendToUser(user.getId(), "notification", NotificationDTO.from(notification));
    }

    public SseEmitter subscribeToStream(String username) {
        User user = findUser(username);
        return sseEmitterRegistry.subscribe(user.getId());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(String username) {
        User user = findUser(username);
        return notificationRepository.findTop100ByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationDTO::from)
                .toList();
    }

    private static final int MAX_PAGE_SIZE = 50;

    @Transactional(readOnly = true)
    public NotificationPageDTO getUserNotificationsPaged(String username, int page, int size) {
        User user = findUser(username);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Slice<Notification> slice = notificationRepository.findByUserOrderByCreatedAtDescIdDesc(user, pageable);
        return NotificationPageDTO.builder()
                .items(slice.getContent().stream().map(NotificationDTO::from).toList())
                .hasMore(slice.hasNext())
                .page(pageable.getPageNumber())
                .unreadCount(notificationRepository.countByUserAndIsReadFalse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        User user = findUser(username);
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        User user = findUser(username);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        notificationRepository.markAsRead(notificationId, LocalDateTime.now());
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = findUser(username);
        notificationRepository.markAllAsReadForUser(user, LocalDateTime.now());
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
