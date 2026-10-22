package com.formation.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import io.jsonwebtoken.ExpiredJwtException;

@SuppressWarnings("null")
class JwtServiceTest {

    private JwtService jwtService;

    // Clé HMAC-SHA256 valide en Base64 (256 bits)
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);
    }

    @Test
    void generateToken_puis_extractUsername_retourne_le_bon_email() {
        UserDetails userDetails = utilisateur("alice@test.com");

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@test.com");
    }

    @Test
    void isTokenValid_retourne_true_pour_token_valide() {
        UserDetails userDetails = utilisateur("bob@test.com");

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_retourne_false_pour_mauvais_utilisateur() {
        String token = jwtService.generateToken(utilisateur("alice@test.com"));

        assertThat(jwtService.isTokenValid(token, utilisateur("eve@test.com"))).isFalse();
    }

    @Test
    void isTokenValid_leve_exception_pour_token_expire() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String token = jwtService.generateToken(utilisateur("carol@test.com"));

        assertThatThrownBy(() -> jwtService.isTokenValid(token, utilisateur("carol@test.com")))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void extractUsername_leve_exception_pour_token_invalide() {
        org.junit.jupiter.api.Assertions.assertThrows(
            Exception.class,
            () -> jwtService.extractUsername("ceci.nest.pas.un.token")
        );
    }

    private UserDetails utilisateur(String email) {
        return User.withUsername(email).password("hash").roles("USER").build();
    }
}
