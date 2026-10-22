package com.formation.security;

import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.repositories.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurDetailsServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private UtilisateurDetailsService service;

    @Test
    void loadUserByUsername_retourne_userdetails_si_email_existe() {
        UtilisateurEntity utilisateur = utilisateur("alice@test.com", "MEMBRE");
        when(utilisateurRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(utilisateur));

        UserDetails result = service.loadUserByUsername("alice@test.com");

        assertThat(result.getUsername()).isEqualTo("alice@test.com");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_MEMBRE");
    }

    @Test
    void loadUserByUsername_leve_exception_si_email_inconnu() {
        when(utilisateurRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("inconnu@test.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("inconnu@test.com");
    }

    @Test
    void loadUserByUsername_retourne_role_admin() {
        UtilisateurEntity utilisateur = utilisateur("admin@test.com", "ADMIN");
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateur));

        UserDetails result = service.loadUserByUsername("admin@test.com");

        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
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
