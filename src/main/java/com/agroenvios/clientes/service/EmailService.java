package com.agroenvios.clientes.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String nombre, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verifica tu cuenta - AgroEnvíos");
            helper.setText(buildVerificationEmailContent(nombre, token), true);

            ClassPathResource logo = new ClassPathResource("static/images/logo.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
            log.info("Correo de verificación enviado a: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error al enviar correo de verificación a {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildVerificationEmailContent(String nombre, String token) {
        Context context = new Context();
        context.setVariable("nombre", nombre);
        context.setVariable("verificationLink", baseUrl + "/confirm-email-client?token=" + token);

        return templateEngine.process("email/verification", context);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String nombre, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Recuperación de contraseña - AgroEnvíos");
            helper.setText(buildPasswordResetEmailContent(nombre, token), true);

            ClassPathResource logo = new ClassPathResource("static/images/logo.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
            log.info("Correo de recuperación enviado a: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error al enviar correo de recuperación a {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildPasswordResetEmailContent(String nombre, String token) {
        Context context = new Context();
        context.setVariable("nombre", nombre);
        context.setVariable("resetLink", baseUrl + "/reset-password-client?token=" + token);

        return templateEngine.process("email/password-reset", context);
    }
}