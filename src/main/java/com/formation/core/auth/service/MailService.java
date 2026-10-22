package com.formation.core.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void envoyerEmailReset(String destinataire, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setSubject("Réinitialisation de votre mot de passe");
        message.setText(
            "Bonjour,\n\n" +
            "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
            "Votre token de réinitialisation : " + token + "\n\n" +
            "Ce token est valable 15 minutes.\n\n" +
            "Utilisez-le avec POST /api/auth/change-password.\n\n" +
            "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email."
        );
        mailSender.send(message);
    }
}
