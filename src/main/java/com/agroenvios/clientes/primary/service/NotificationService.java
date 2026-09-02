package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.notification.NotificationDTO;
import com.agroenvios.clientes.primary.model.Notification;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.NotificationRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Persiste la notificación para la lista en la app. Se llama siempre que hay un
     * cambio de estado de pedido con copy definido, independientemente de si el
     * usuario tiene push token registrado (eso solo afecta si además le llega el push).
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
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(String username) {
        User user = findUser(username);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationDTO::from)
                .toList();
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
