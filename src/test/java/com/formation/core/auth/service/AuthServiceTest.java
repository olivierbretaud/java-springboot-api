package com.formation.core.auth.service;

import com.formation.core.auth.dto.AuthResponse;
import com.formation.core.auth.dto.ChangePasswordRequest;
import com.formation.core.auth.dto.LoginRequest;
import com.formation.core.auth.dto.ResetPasswordRequest;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.exceptions.ResetTokenInvalidException;
import com.formation.engine.repositories.UtilisateurRepository;
import com.formation.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @InjectMocks
    private AuthService authService;

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_retourne_token_si_credentials_valides() {
        UserDetails userDetails = User.withUsername("alice@test.com").password("hash").roles("USER").build();
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtService.generateToken(userDetails)).thenReturn("eyJ...");

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@test.com");
        request.setMotDePasse("secret");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("eyJ...");
    }

    @Test
    void login_leve_BadCredentialsException_si_credentials_invalides() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@test.com");
        request.setMotDePasse("mauvais");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_envoie_email_si_utilisateur_existe() {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        utilisateur.setEmail("alice@test.com");

        when(utilisateurRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any())).thenReturn(utilisateur);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("alice@test.com");

        authService.resetPassword(request);

        ArgumentCaptor<UtilisateurEntity> captor = ArgumentCaptor.forClass(UtilisateurEntity.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getResetToken()).isNotBlank();
        assertThat(captor.getValue().getResetTokenExpiry()).isAfter(LocalDateTime.now());
        verify(mailService).envoyerEmailReset(eq("alice@test.com"), any());
    }

    @Test
    void resetPassword_ne_leve_pas_exception_si_email_inconnu() {
        when(utilisateurRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("inconnu@test.com");

        authService.resetPassword(request);

        verify(utilisateurRepository, never()).save(any());
        verify(mailService, never()).envoyerEmailReset(any(), any());
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    void changePassword_met_a_jour_mot_de_passe_si_token_valide() {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        utilisateur.setResetToken("token-valide");
        utilisateur.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(utilisateurRepository.findByResetToken("token-valide")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.encode("NouveauMdp1@")).thenReturn("$2a$hash");
        when(utilisateurRepository.save(any())).thenReturn(utilisateur);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setToken("token-valide");
        request.setNouveauMotDePasse("NouveauMdp1@");

        authService.changePassword(request);

        ArgumentCaptor<UtilisateurEntity> captor = ArgumentCaptor.forClass(UtilisateurEntity.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getMotDePasse()).isEqualTo("$2a$hash");
        assertThat(captor.getValue().getResetToken()).isNull();
        assertThat(captor.getValue().getResetTokenExpiry()).isNull();
    }

    @Test
    void changePassword_leve_exception_si_token_inconnu() {
        when(utilisateurRepository.findByResetToken("inconnu")).thenReturn(Optional.empty());

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setToken("inconnu");
        request.setNouveauMotDePasse("NouveauMdp1@");

        assertThrows(ResetTokenInvalidException.class, () -> authService.changePassword(request));
    }

    @Test
    void changePassword_leve_exception_si_token_expire() {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        utilisateur.setResetToken("token-expire");
        utilisateur.setResetTokenExpiry(LocalDateTime.now().minusMinutes(5));

        when(utilisateurRepository.findByResetToken("token-expire")).thenReturn(Optional.of(utilisateur));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setToken("token-expire");
        request.setNouveauMotDePasse("NouveauMdp1@");

        ResetTokenInvalidException ex = assertThrows(
                ResetTokenInvalidException.class,
                () -> authService.changePassword(request)
        );
        assertThat(ex.getMessage()).contains("expiré");
    }
}
