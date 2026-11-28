package com.biblosmast.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService implements ICorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final ObjectProvider<JavaMailSender> proveedorCorreo;

    @Value("${app.correo.remitente:no-reply@biblosmast.local}")
    private String remitente;

    public CorreoService(ObjectProvider<JavaMailSender> proveedorCorreo) {
        this.proveedorCorreo = proveedorCorreo;
    }

    @Override
    public void enviar(String para, String asunto, String texto) {
        JavaMailSender emisor = proveedorCorreo.getIfAvailable();
        if (emisor == null) {
            // Modo desarrollo: no hay servidor SMTP configurado (spring.mail.host)
            log.warn("(modo desarrollo: no hay servidor de correo configurado)\nPara: {}\nAsunto: {}\n{}", para, asunto, texto);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            emisor.send(mensaje);
        } catch (MailException e) {
            // Un fallo del correo no debe revelar si la cuenta existe ni romper la petición
            log.error("No se pudo enviar el correo a {}: {}", para, e.getMessage());
        }
    }
}
