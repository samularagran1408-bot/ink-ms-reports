package com.inklusport.reports.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envía reportes programados por correo electrónico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * Envía el PDF del reporte semanal. Si el correo está deshabilitado, solo registra el intento.
     *
     * @param to       destinatario
     * @param subject  asunto
     * @param bodyHtml cuerpo HTML
     * @param pdfBytes adjunto PDF (opcional)
     * @param filename nombre del adjunto
     * @return true si SMTP aceptó el mensaje o si el correo está deshabilitado (modo dry-run)
     */
    public boolean sendWeeklyReport(String to, String subject, String bodyHtml, byte[] pdfBytes, String filename) {
        if (!mailEnabled) {
            log.info("mail.enabled=false: reporte semanal no enviado a {} (dry-run OK)", to);
            return true;
        }
        if (to == null || !to.contains("@") || fromEmail == null || fromEmail.isBlank()) {
            log.warn("No se puede enviar reporte semanal: destinatario/remitente inválido");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);
            if (pdfBytes != null && pdfBytes.length > 0) {
                helper.addAttachment(
                        filename != null ? filename : "reporte-semanal.pdf",
                        new ByteArrayResource(pdfBytes)
                );
            }
            mailSender.send(message);
            log.info("Reporte semanal enviado a {}", to);
            return true;
        } catch (Exception e) {
            log.error("Error enviando reporte semanal a {}: {}", to, e.getMessage());
            return false;
        }
    }
}
