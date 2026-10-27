package com.formation.security;

import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.UtilisateurEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UtilisateurDetailsService utilisateurDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sans_header_authorization_passe_la_requete_sans_authentifier() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void header_sans_bearer_passe_la_requete_sans_authentifier() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void token_valide_place_authentication_dans_le_context() throws Exception {
        UtilisateurEntity utilisateur = utilisateur("alice@test.com", "MEMBRE");

        when(request.getHeader("Authorization")).thenReturn("Bearer token.valide.ici");
        when(jwtService.extractUsername("token.valide.ici")).thenReturn("alice@test.com");
        when(utilisateurDetailsService.loadUserByUsername("alice@test.com")).thenReturn(utilisateur);
        when(jwtService.isTokenValid("token.valide.ici", utilisateur)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice@test.com");
    }

    @Test
    void token_invalide_ne_place_pas_authentication_dans_le_context() throws Exception {
        UtilisateurEntity utilisateur = utilisateur("alice@test.com", "MEMBRE");

        when(request.getHeader("Authorization")).thenReturn("Bearer token.expire");
        when(jwtService.extractUsername("token.expire")).thenReturn("alice@test.com");
        when(utilisateurDetailsService.loadUserByUsername("alice@test.com")).thenReturn(utilisateur);
        when(jwtService.isTokenValid("token.expire", utilisateur)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private UtilisateurEntity utilisateur(String email, String roleCode) {
        RoleEntity role = new RoleEntity();
        role.setCode(roleCode);
        UtilisateurEntity u = new UtilisateurEntity();
        u.setEmail(email);
        u.setMotDePasse("hash");
        u.setNom("Test");
        u.setPrenom("User");
        u.setRole(role);
        return u;
    }
}
