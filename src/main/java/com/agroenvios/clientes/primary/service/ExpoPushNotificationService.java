package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpoPushNotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Async
    public void sendPedidoNotification(User user, String estado, Long pedidoId) {
        String pushToken = user.getPushToken();
        if (pushToken == null || pushToken.isBlank()) {
            return;
        }

        String title = getTitulo(estado);
        if (title == null) {
            return;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            if (pedidoId != null) {
                data.put("pedidoId", pedidoId);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", pushToken);
            payload.put("title", title);
            payload.put("body", getMensaje(estado, pedidoId));
            payload.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    EXPO_PUSH_URL,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            if (response.getBody() != null && response.getBody().contains("DeviceNotRegistered")) {
                log.info("Push token inválido para usuario={}, limpiando token", user.getUsername());
                userRepository.findByUsername(user.getUsername()).ifPresent(u -> {
                    u.setPushToken(null);
                    userRepository.save(u);
                });
            }
        } catch (Exception e) {
            log.warn("Error al enviar notificación push a usuario={}: {}", user.getUsername(), e.getMessage());
        }
    }

    private String getTitulo(String estado) {
        return switch (estado) {
            case "APROBADO" -> "Pedido confirmado";
            case "RECHAZADO" -> "Pedido rechazado";
            case "CANCELADO" -> "Pedido cancelado";
            default -> null;
        };
    }

    private String getMensaje(String estado, Long pedidoId) {
        String id = pedidoId != null ? "#" + pedidoId : "";
        return switch (estado) {
            case "APROBADO" -> "Tu pedido " + id + " fue aprobado y está en preparación";
            case "RECHAZADO" -> "Tu pedido " + id + " no pudo ser procesado";
            case "CANCELADO" -> "Tu pedido " + id + " fue cancelado";
            default -> "";
        };
    }
}
