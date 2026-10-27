package com.formation.core.auth.controller;

import com.formation.core.auth.dto.AuthResponse;
import com.formation.core.auth.service.AuthService;
import com.formation.core.auth.service.RefreshTokenService;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.exceptions.ResetTokenInvalidException;
import com.formation.security.TestSecurityConfig;
import com.formation.security.UtilisateurDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UtilisateurDetailsService utilisateurDetailsService;

    @MockitoBean
    private com.formation.security.JwtService jwtService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Test
    void login_retourne_200_et_token_si_credentials_valides() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("eyJ...", "refresh-uuid", 3600));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@test.com\",\"motDePasse\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("eyJ..."))
                .andExpect(jsonPath("$.refreshToken").value("refresh-uuid"));
    }

    @Test
    void login_retourne_401_si_credentials_invalides() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@test.com\",\"motDePasse\":\"mauvais\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_retourne_400_si_email_invalide() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pas-un-email\",\"motDePasse\":\"secret\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/refresh ────────────────────────────────────────────────────

    @Test
    void refresh_retourne_200_et_nouveau_token_si_refresh_valide() throws Exception {
        when(authService.refresh("refresh-uuid")).thenReturn(new AuthResponse("nouveau-token", "refresh-uuid", 3600));

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-uuid\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("nouveau-token"));
    }

    @Test
    void refresh_retourne_400_si_refresh_token_absent() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/logout ─────────────────────────────────────────────────────

    @Test
    void logout_retourne_204_et_revoque_le_token() throws Exception {
        doNothing().when(authService).logout("refresh-uuid");

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-uuid\"}"))
                .andExpect(status().isNoContent());

        verify(authService).logout("refresh-uuid");
    }

    // ── POST /auth/reset-password ─────────────────────────────────────────────

    @Test
    void resetPassword_retourne_200_avec_message_si_email_valide() throws Exception {
        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                    "Si un compte est associé à cet email, un lien de réinitialisation vous a été envoyé."
                ));
    }

    @Test
    void resetPassword_retourne_400_si_email_invalide() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pas-un-email\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/change-password ────────────────────────────────────────────

    @Test
    void changePassword_retourne_200_avec_message_si_token_valide() throws Exception {
        doNothing().when(authService).changePassword(any());

        mockMvc.perform(post("/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"uuid-token\",\"nouveauMotDePasse\":\"NouveauMdp1@\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                    "Votre mot de passe a été réinitialisé avec succès. Vous pouvez maintenant vous connecter."
                ));
    }

    @Test
    void changePassword_retourne_400_si_token_invalide() throws Exception {
        doThrow(new ResetTokenInvalidException("Token de réinitialisation invalide."))
                .when(authService).changePassword(any());

        mockMvc.perform(post("/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"mauvais-token\",\"nouveauMotDePasse\":\"NouveauMdp1@\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Token de réinitialisation invalide."));
    }

    @Test
    void changePassword_retourne_400_si_mot_de_passe_trop_faible() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"uuid-token\",\"nouveauMotDePasse\":\"faible\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /auth/profile ─────────────────────────────────────────────────────

    @Test
    void profile_retourne_200_avec_donnees_utilisateur() throws Exception {
        RoleEntity role = new RoleEntity();
        role.setCode("MEMBRE");

        UtilisateurEntity utilisateur = new UtilisateurEntity();
        utilisateur.setId(1L);
        utilisateur.setEmail("alice@test.com");
        utilisateur.setNom("Alice");
        utilisateur.setPrenom("Dupont");
        utilisateur.setRole(role);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                utilisateur, null, utilisateur.getAuthorities()
        );

        mockMvc.perform(get("/auth/profile").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.nom").value("Alice"))
                .andExpect(jsonPath("$.prenom").value("Dupont"))
                .andExpect(jsonPath("$.role").value("MEMBRE"));
    }
}
