package com.formation.core.auth.service;

import com.formation.engine.entities.RefreshTokenEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.exceptions.ResetTokenInvalidException;
import com.formation.engine.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationDays", 7);
    }

    // --- creer() ---

    @Test
    void creer_revoque_les_anciens_tokens_de_utilisateur() {
        UtilisateurEntity utilisateur = utilisateur(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.creer(utilisateur);

        verify(refreshTokenRepository).revoquerParUtilisateur(1L);
    }

    @Test
    void creer_sauvegarde_un_nouveau_token_avec_expiration() {
        UtilisateurEntity utilisateur = utilisateur(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RefreshTokenEntity result = refreshTokenService.creer(utilisateur);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshTokenEntity saved = captor.getValue();
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getUtilisateur()).isEqualTo(utilisateur);
        assertThat(saved.getExpiration()).isAfter(LocalDateTime.now(ZoneOffset.UTC).plusDays(6));
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void creer_genere_un_token_uuid_unique() {
        UtilisateurEntity utilisateur = utilisateur(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RefreshTokenEntity t1 = refreshTokenService.creer(utilisateur);
        RefreshTokenEntity t2 = refreshTokenService.creer(utilisateur);

        assertThat(t1.getToken()).isNotEqualTo(t2.getToken());
    }

    // --- valider() ---

    @Test
    void valider_retourne_le_token_si_valide() {
        RefreshTokenEntity rt = tokenValide();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(rt));

        RefreshTokenEntity result = refreshTokenService.valider("abc");

        assertThat(result).isEqualTo(rt);
    }

    @Test
    void valider_leve_exception_si_token_introuvable() {
        when(refreshTokenRepository.findByToken("inconnu")).thenReturn(Optional.empty());

        assertThrows(ResetTokenInvalidException.class,
                () -> refreshTokenService.valider("inconnu"));
    }

    @Test
    void valider_leve_exception_si_token_revoque() {
        RefreshTokenEntity rt = tokenValide();
        rt.setRevoque(true);
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(rt));

        ResetTokenInvalidException ex = assertThrows(ResetTokenInvalidException.class,
                () -> refreshTokenService.valider("abc"));

        assertThat(ex.getMessage()).contains("révoqué");
    }

    @Test
    void valider_leve_exception_si_token_expire() {
        RefreshTokenEntity rt = tokenValide();
        rt.setExpiration(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(rt));

        ResetTokenInvalidException ex = assertThrows(ResetTokenInvalidException.class,
                () -> refreshTokenService.valider("abc"));

        assertThat(ex.getMessage()).contains("expiré");
    }

    // --- revoquer() ---

    @Test
    void revoquer_marque_le_token_comme_revoque() {
        RefreshTokenEntity rt = tokenValide();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(rt));

        refreshTokenService.revoquer("abc");

        assertThat(rt.isRevoque()).isTrue();
        verify(refreshTokenRepository).save(rt);
    }

    @Test
    void revoquer_ne_fait_rien_si_token_introuvable() {
        when(refreshTokenRepository.findByToken("inconnu")).thenReturn(Optional.empty());

        refreshTokenService.revoquer("inconnu");

        verify(refreshTokenRepository, never()).save(any());
    }

    // --- helpers ---

    private UtilisateurEntity utilisateur(Long id) {
        UtilisateurEntity u = new UtilisateurEntity();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private RefreshTokenEntity tokenValide() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("abc");
        rt.setExpiration(LocalDateTime.now(ZoneOffset.UTC).plusDays(7));
        rt.setRevoque(false);
        return rt;
    }
}
