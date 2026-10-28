package com.example.Foodies.Moderacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de emails de moderación con {@link JavaMailSender}.
 *
 * <p>En desarrollo apunta a Mailtrap (ver {@code spring.mail.*} en {@code application.properties});
 * para producción alcanza con cambiar las variables {@code MAIL_*}. Si no hay servidor configurado o
 * el envío falla, se registra en el log y se devuelve {@code false}: un problema de correo nunca
 * revierte la sanción.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${foodies.mail.from:no-reply@foodies.com}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    /**
     * Envía el aviso de sanción.
     *
     * @param destinatario email del usuario o del encargado del restaurante
     * @param nombre       nombre con el que se saluda
     * @param strikes      strikes acumulados después de esta sanción
     * @param limite       strikes a partir de los cuales la cuenta puede darse de baja
     * @param motivo       motivo que escribió el administrador
     * @return {@code true} si el servidor de correo aceptó el mensaje
     */
    public boolean enviarAvisoSancion(String destinatario, String nombre, int strikes, int limite, String motivo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("No hay servidor de correo configurado; no se envió el aviso de sanción a {}", destinatario);
            return false;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(PlantillaAvisoSancion.asunto(strikes, limite));
        mensaje.setText(PlantillaAvisoSancion.cuerpo(nombre, strikes, limite, motivo));
        try {
            sender.send(mensaje);
            return true;
        } catch (MailException ex) {
            log.error("No se pudo enviar el aviso de sanción a {}: {}", destinatario, ex.getMessage());
            return false;
        }
    }
}
