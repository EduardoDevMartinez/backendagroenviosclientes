package com.agroenvios.clientes.primary.listener;

import com.agroenvios.clientes.primary.events.UserRegisteredEvent;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.service.EmailService;
import com.agroenvios.clientes.primary.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredListener {

    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    @EventListener
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        User user = event.getUser();
        log.info("Evento de registro recibido para usuario: {}", user.getUsername());

        String verificationToken = emailVerificationService.generateVerificationToken(user.getUsername());
        emailService.sendVerificationEmail(user.getCorreo(), user.getNombre(), verificationToken);
    }
}