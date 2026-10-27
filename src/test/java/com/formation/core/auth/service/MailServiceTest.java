package com.formation.core.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    // ── envoyerEmailReset ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void envoyerEmailReset_appelle_mailSender_send() {
        mailService.envoyerEmailReset("alice@test.com", "tok-123");

        verify(mailSender).send(org.mockito.ArgumentMatchers.<SimpleMailMessage>any());
    }

    @Test
    void envoyerEmailReset_definit_le_bon_destinataire() {
        mailService.envoyerEmailReset("alice@test.com", "tok-123");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getTo()).containsExactly("alice@test.com");
    }

    @Test
    void envoyerEmailReset_definit_le_bon_sujet() {
        mailService.envoyerEmailReset("alice@test.com", "tok-123");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getSubject()).isEqualTo("Réinitialisation de votre mot de passe");
    }

    @Test
    void envoyerEmailReset_inclut_le_token_dans_le_corps() {
        mailService.envoyerEmailReset("alice@test.com", "tok-123");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getText()).contains("tok-123");
    }

    @Test
    void envoyerEmailReset_inclut_validite_15_minutes() {
        mailService.envoyerEmailReset("alice@test.com", "tok-123");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getText()).contains("15 minutes");
    }

    @Test
    void envoyerEmailReset_inclut_endpoint_change_password() {
        mailService.envoyerEmailReset("alice@test.com", "tok-123");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getText()).contains("/api/auth/change-password");
    }

    // ── envoyerEmailActivation ────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void envoyerEmailActivation_appelle_mailSender_send() {
        mailService.envoyerEmailActivation("bob@test.com", "tok-456");

        verify(mailSender).send(org.mockito.ArgumentMatchers.<SimpleMailMessage>any());
    }

    @Test
    void envoyerEmailActivation_definit_le_bon_destinataire() {
        mailService.envoyerEmailActivation("bob@test.com", "tok-456");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getTo()).containsExactly("bob@test.com");
    }

    @Test
    void envoyerEmailActivation_definit_le_bon_sujet() {
        mailService.envoyerEmailActivation("bob@test.com", "tok-456");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getSubject()).isEqualTo("Activation de votre compte");
    }

    @Test
    void envoyerEmailActivation_inclut_le_token_dans_le_corps() {
        mailService.envoyerEmailActivation("bob@test.com", "tok-456");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getText()).contains("tok-456");
    }

    @Test
    void envoyerEmailActivation_inclut_validite_24_heures() {
        mailService.envoyerEmailActivation("bob@test.com", "tok-456");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getText()).contains("24 heures");
    }

    @Test
    void envoyerEmailActivation_inclut_endpoint_change_password() {
        mailService.envoyerEmailActivation("bob@test.com", "tok-456");

        SimpleMailMessage message = captureMessage();
        assertThat(message.getText()).contains("/api/auth/change-password");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    @SuppressWarnings("null")
    private SimpleMailMessage captureMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
