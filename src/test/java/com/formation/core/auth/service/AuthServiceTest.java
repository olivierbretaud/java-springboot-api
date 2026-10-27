package com.formation.core.auth.service;

import com.formation.core.auth.dto.AuthResponse;
import com.formation.core.auth.dto.ChangePasswordRequest;
import com.formation.core.auth.dto.LoginRequest;
import com.formation.core.auth.dto.ResetPasswordRequest;
import com.formation.engine.entities.RefreshTokenEntity;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.exceptions.CompteBloqueException;
import com.formation.engine.exceptions.ResetTokenInvalidException;
import com.formation.engine.repositories.UtilisateurRepository;
import com.formation.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_retourne_token_si_credentials_valides() {
        UtilisateurEntity utilisateur = utilisateurAvecRole("alice@test.com");

        when(utilisateurRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("secret", utilisateur.getPassword())).thenReturn(true);
        when(jwtService.generateToken(utilisateur)).thenReturn("eyJ...");

        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("refresh-uuid");
        when(refreshTokenService.creer(utilisateur)).thenReturn(rt);

        LoginRequest request = loginRequest("alice@test.com", "secret");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("eyJ...");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid");
    }

    @Test
    void login_leve_BadCredentialsException_si_email_inconnu() {
        when(utilisateurRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest("inconnu@test.com", "secret")));
    }

    @Test
    void login_leve_BadCredentialsException_si_mot_de_passe_incorrect() {
        UtilisateurEntity utilisateur = utilisateurAvecRole("alice@test.com");

        when(utilisateurRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("mauvais", utilisateur.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest("alice@test.com", "mauvais")));
    }

    @Test
    void login_incremente_compteur_apres_echec() {
        UtilisateurEntity utilisateur = utilisateurAvecRole("alice@test.com");

        when(utilisateurRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest("alice@test.com", "mauvais")));

        ArgumentCaptor<UtilisateurEntity> captor = ArgumentCaptor.forClass(UtilisateurEntity.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getTentativesEchouees()).isEqualTo(1);
    }

    @Test
    void login_bloque_le_compte_apres_5_echecs() {
        UtilisateurEntity utilisateur = utilisateurAvecRole("alice@test.com");
        utilisateur.setTentativesEchouees(4);

        when(utilisateurRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest("alice@test.com", "mauvais")));

        ArgumentCaptor<UtilisateurEntity> captor = ArgumentCaptor.forClass(UtilisateurEntity.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getBlocageJusqua()).isNotNull();
        assertThat(captor.getValue().getBlocageJusqua()).isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void login_leve_exception_si_compte_bloque() {
        UtilisateurEntity utilisateur = utilisateurAvecRole("alice@test.com");
        utilisateur.setTentativesEchouees(5);
        utilisateur.setBlocageJusqua(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));

        when(utilisateurRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(utilisateur));

        CompteBloqueException ex = assertThrows(CompteBloqueException.class,
                () -> authService.login(loginRequest("alice@test.com", "secret")));

        assertThat(ex.getMessage()).contains("bloqué");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_reinitialise_compteur_apres_succes() {
        UtilisateurEntity utilisateur = utilisateurAvecRole("alice@test.com");
        utilisateur.setTentativesEchouees(3);

        when(utilisateurRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("secret", utilisateur.getPassword())).thenReturn(true);
        when(jwtService.generateToken(utilisateur)).thenReturn("eyJ...");

        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("refresh-uuid");
        when(refreshTokenService.creer(utilisateur)).thenReturn(rt);

        authService.login(loginRequest("alice@test.com", "secret"));

        ArgumentCaptor<UtilisateurEntity> captor = ArgumentCaptor.forClass(UtilisateurEntity.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getTentativesEchouees()).isEqualTo(0);
        assertThat(captor.getValue().getBlocageJusqua()).isNull();
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_retourne_nouveau_token_si_refresh_valide() {
        UtilisateurEntity utilisateur = utilisateurAvecRole("alice@test.com");
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("refresh-uuid");
        rt.setUtilisateur(utilisateur);

        when(refreshTokenService.valider("refresh-uuid")).thenReturn(rt);
        when(jwtService.generateToken(utilisateur)).thenReturn("nouveau-token");

        AuthResponse response = authService.refresh("refresh-uuid");

        assertThat(response.getToken()).isEqualTo("nouveau-token");
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
        assertThat(captor.getValue().getResetTokenExpiry()).isAfter(LocalDateTime.now(ZoneOffset.UTC));
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
        utilisateur.setResetTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));

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
        utilisateur.setResetTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5));

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

    // ── helpers ───────────────────────────────────────────────────────────────

    private UtilisateurEntity utilisateurAvecRole(String email) {
        RoleEntity role = new RoleEntity();
        role.setCode("USER");

        UtilisateurEntity u = new UtilisateurEntity();
        u.setEmail(email);
        u.setMotDePasse("$2a$hash");
        u.setActif(true);
        u.setRole(role);
        return u;
    }

    private LoginRequest loginRequest(String email, String motDePasse) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setMotDePasse(motDePasse);
        return r;
    }
}
