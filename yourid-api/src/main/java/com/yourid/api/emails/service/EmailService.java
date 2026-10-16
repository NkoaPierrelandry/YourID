package com.yourid.api.emails.service;

import com.yourid.api.emails.entity.EmailLog;
import com.yourid.api.emails.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi d emails via SMTP -- configure pour utiliser le relai SMTP de Brevo
 * (voir application.yml : spring.mail.host=smtp-relay.brevo.com).
 * Chaque envoi est trace dans EmailLog, reussite ou echec, comme le faisait
 * le backend NestJS d origine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public void sendOrderConfirmation(String toEmail, String customerName, String storeName, String orderNumber) {
        String subject = "Confirmation de votre commande " + orderNumber;
        String body = "Bonjour " + customerName + ",\n\n"
                + "Merci pour votre achat aupres de " + storeName + " !\n"
                + "Votre commande " + orderNumber + " a ete confirmee.\n\n"
                + "Vous pouvez retrouver vos produits depuis votre espace client.\n\n"
                + "L equipe YourID";
        send(toEmail, subject, body, "order_confirmation");
    }

    public void sendPasswordReset(String toEmail, String resetLink) {
        String subject = "Reinitialisation de votre mot de passe";
        String body = "Cliquez sur ce lien pour reinitialiser votre mot de passe :\n" + resetLink
                + "\n\nCe lien expire dans 1 heure. Si vous n etes pas a l origine de cette demande, ignorez cet email.";
        send(toEmail, subject, body, "password_reset");
    }

    private void send(String to, String subject, String body, String template) {
        EmailLog.EmailLogBuilder logBuilder = EmailLog.builder()
                .recipient(to)
                .subject(subject)
                .template(template);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromAddress + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            emailLogRepository.save(logBuilder.status("sent").build());
        } catch (Exception e) {
            // Log complet (avec cause chainee) plutot que juste le message, pour voir
            // la vraie reponse du serveur SMTP en cas d echec d authentification.
            log.error("Echec envoi email a {} (template {})", to, template, e);
            String detail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            emailLogRepository.save(logBuilder.status("failed").error(detail).build());
        }
    }
}
