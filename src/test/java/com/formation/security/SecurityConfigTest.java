package com.formation.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigTest.MinimalController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@SuppressWarnings("null")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockitoBean
    private UtilisateurDetailsService utilisateurDetailsService;

    @BeforeEach
    void configurerFiltre() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @RestController
    static class MinimalController {
        @GetMapping("/resource-protegee")
        String ressourceProtegee() {
            return "ok";
        }
    }

    // ── Routes publiques Auth ─────────────────────────────────────────────────

    @Test
    void route_login_accessible_sans_token() throws Exception {
        mockMvc.perform(post("/auth/login"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void route_reset_password_accessible_sans_token() throws Exception {
        mockMvc.perform(post("/auth/reset-password"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void route_change_password_accessible_sans_token() throws Exception {
        mockMvc.perform(post("/auth/change-password"))
                .andExpect(status().is(not(401)));
    }

    // ── Routes Swagger ────────────────────────────────────────────────────────

    @Test
    void swagger_ui_accessible_sans_token() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void swagger_api_docs_accessible_sans_token() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is(not(401)));
    }

    // ── Routes protégées ──────────────────────────────────────────────────────

    @Test
    void route_protegee_retourne_401_sans_token() throws Exception {
        mockMvc.perform(get("/resource-protegee"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void route_protegee_non_bloquee_avec_utilisateur_authentifie() throws Exception {
        mockMvc.perform(get("/resource-protegee"))
                .andExpect(status().is(not(401)));
    }

    // ── CSRF et session ───────────────────────────────────────────────────────

    @Test
    void csrf_desactive_post_sans_token_csrf_ne_retourne_pas_403() throws Exception {
        mockMvc.perform(post("/auth/login"))
                .andExpect(status().is(not(403)));
    }

    @Test
    void session_non_creee_stateless() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    // ── Bean passwordEncoder ──────────────────────────────────────────────────

    @Test
    void passwordEncoder_est_BCrypt() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encode_et_verifie_un_mot_de_passe() {
        String motDePasse = "MonMotDePasse!42";
        String hash = passwordEncoder.encode(motDePasse);
        assertThat(hash).isNotEqualTo(motDePasse);
        assertThat(passwordEncoder.matches(motDePasse, hash)).isTrue();
    }
}
