package com.formation.core.auth.service;

import com.formation.engine.entities.RefreshTokenEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.exceptions.ResetTokenInvalidException;
import com.formation.engine.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    public RefreshTokenEntity creer(UtilisateurEntity utilisateur) {
        refreshTokenRepository.revoquerParUtilisateur(utilisateur.getId());

        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setToken(UUID.randomUUID().toString());
        token.setUtilisateur(utilisateur);
        token.setExpiration(LocalDateTime.now(ZoneOffset.UTC).plusDays(refreshExpirationDays));
        return refreshTokenRepository.save(token);
    }

    public RefreshTokenEntity valider(String token) {
        RefreshTokenEntity rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResetTokenInvalidException("Refresh token introuvable."));

        if (rt.isRevoque() || rt.isExpire()) {
            throw new ResetTokenInvalidException("Refresh token expiré ou révoqué.");
        }
        return rt;
    }

    public void revoquer(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoque(true);
            refreshTokenRepository.save(rt);
        });
    }
}
