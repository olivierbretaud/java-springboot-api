package com.formation.core.auth.service;

import com.formation.core.auth.dto.AuthResponse;
import com.formation.core.auth.dto.ChangePasswordRequest;
import com.formation.core.auth.dto.LoginRequest;
import com.formation.core.auth.dto.ResetPasswordRequest;
import com.formation.engine.entities.RefreshTokenEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.exceptions.CompteBloqueException;
import com.formation.engine.exceptions.ResetTokenInvalidException;
import com.formation.engine.repositories.UtilisateurRepository;
import com.formation.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_TENTATIVES = 5;
    private static final int BLOCAGE_MINUTES = 15;

    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    public AuthResponse login(LoginRequest request) {
        UtilisateurEntity utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect."));

        if (utilisateur.estBloque()) {
            throw new CompteBloqueException("Compte temporairement bloqué. Réessayez dans quelques minutes.");
        }

        if (!passwordEncoder.matches(request.getMotDePasse(), utilisateur.getPassword())) {
            enregistrerEchec(utilisateur);
            throw new BadCredentialsException("Email ou mot de passe incorrect.");
        }

        reinitialiserCompteur(utilisateur);

        String accessToken = jwtService.generateToken(utilisateur);
        RefreshTokenEntity refreshToken = refreshTokenService.creer(utilisateur);

        return new AuthResponse(accessToken, refreshToken.getToken(), expirationMs / 1000);
    }

    public AuthResponse refresh(String refreshToken) {
        RefreshTokenEntity rt = refreshTokenService.valider(refreshToken);
        String newAccessToken = jwtService.generateToken(rt.getUtilisateur());
        return new AuthResponse(newAccessToken, refreshToken, expirationMs / 1000);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoquer(refreshToken);
    }

    public void resetPassword(ResetPasswordRequest request) {
        Optional<UtilisateurEntity> optUtilisateur = utilisateurRepository.findByEmail(request.getEmail());
        if (optUtilisateur.isEmpty()) {
            return;
        }
        UtilisateurEntity utilisateur = optUtilisateur.get();
        String token = UUID.randomUUID().toString();
        utilisateur.setResetToken(token);
        utilisateur.setResetTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15));
        utilisateurRepository.save(utilisateur);
        mailService.envoyerEmailReset(utilisateur.getEmail(), token);
    }

    public void changePassword(ChangePasswordRequest request) {
        UtilisateurEntity utilisateur = utilisateurRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new ResetTokenInvalidException("Token de réinitialisation invalide."));

        if (utilisateur.getResetTokenExpiry() == null ||
                utilisateur.getResetTokenExpiry().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ResetTokenInvalidException("Le token a expiré.");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateur.setResetToken(null);
        utilisateur.setResetTokenExpiry(null);
        if (!utilisateur.isActif()) {
            utilisateur.setActif(true);
        }
        utilisateurRepository.save(utilisateur);
    }

    private void enregistrerEchec(UtilisateurEntity utilisateur) {
        int tentatives = utilisateur.getTentativesEchouees() + 1;
        utilisateur.setTentativesEchouees(tentatives);

        if (tentatives >= MAX_TENTATIVES) {
            utilisateur.setBlocageJusqua(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(BLOCAGE_MINUTES));
            log.warn("Compte {} bloqué après {} tentatives échouées", utilisateur.getEmail(), tentatives);
        }
        utilisateurRepository.save(utilisateur);
    }

    private void reinitialiserCompteur(UtilisateurEntity utilisateur) {
        if (utilisateur.getTentativesEchouees() > 0 || utilisateur.getBlocageJusqua() != null) {
            utilisateur.setTentativesEchouees(0);
            utilisateur.setBlocageJusqua(null);
            utilisateurRepository.save(utilisateur);
        }
    }
}
