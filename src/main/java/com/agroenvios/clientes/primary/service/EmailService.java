package com.agroenvios.clientes.primary.service;

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

import java.util.List;

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

    public record LineaFactura(String nombre, String cantidad, String subtotal) {}

    /**
     * Avisa a un comercio que un cliente pidió factura de su pedido. El Reply-To es el correo
     * del cliente: el comercio le contesta directo para pedirle sus datos fiscales.
     */
    @Async
    public void sendInvoiceRequestEmail(String toEmail, String comercio, Long pedidoId, String fechaPedido,
                                        String clienteNombre, String clienteCorreo, String clienteTelefono,
                                        List<LineaFactura> lineas, String totalComercio, String fechaLimite) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setReplyTo(clienteCorreo);
            helper.setSubject("Solicitud de factura - Pedido #" + pedidoId + " - AgroEnvíos");

            Context context = new Context();
            context.setVariable("comercio", comercio);
            context.setVariable("pedidoId", pedidoId);
            context.setVariable("fechaPedido", fechaPedido);
            context.setVariable("clienteNombre", clienteNombre);
            context.setVariable("clienteCorreo", clienteCorreo);
            context.setVariable("clienteTelefono", clienteTelefono);
            context.setVariable("lineas", lineas);
            context.setVariable("totalComercio", totalComercio);
            context.setVariable("fechaLimite", fechaLimite);
            helper.setText(templateEngine.process("email/invoice-request", context), true);

            ClassPathResource logo = new ClassPathResource("static/images/logo.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
            log.info("Solicitud de factura del pedido {} enviada a {}", pedidoId, toEmail);
        } catch (MessagingException e) {
            log.error("Error al enviar solicitud de factura del pedido {} a {}: {}", pedidoId, toEmail, e.getMessage());
        }
    }

    private String buildPasswordResetEmailContent(String nombre, String token) {
        Context context = new Context();
        context.setVariable("nombre", nombre);
        context.setVariable("resetLink", baseUrl + "/reset-password-client?token=" + token);

        return templateEngine.process("email/password-reset", context);
    }
}