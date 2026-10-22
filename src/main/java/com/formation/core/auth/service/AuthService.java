package com.formation.core.auth.service;

import com.formation.core.auth.dto.AuthResponse;
import com.formation.core.auth.dto.ChangePasswordRequest;
import com.formation.core.auth.dto.LoginRequest;
import com.formation.core.auth.dto.ResetPasswordRequest;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.exceptions.ResetTokenInvalidException;
import com.formation.engine.repositories.UtilisateurRepository;
import com.formation.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );
        UserDetails utilisateur = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(utilisateur);
        return new AuthResponse(token);
    }

    public void resetPassword(ResetPasswordRequest request) {
        Optional<UtilisateurEntity> optUtilisateur = utilisateurRepository.findByEmail(request.getEmail());
        if (optUtilisateur.isEmpty()) {
            return;
        }
        UtilisateurEntity utilisateur = optUtilisateur.get();
        String token = UUID.randomUUID().toString();
        utilisateur.setResetToken(token);
        utilisateur.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        utilisateurRepository.save(utilisateur);
        mailService.envoyerEmailReset(utilisateur.getEmail(), token);
    }

    public void changePassword(ChangePasswordRequest request) {
        UtilisateurEntity utilisateur = utilisateurRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new ResetTokenInvalidException("Token de réinitialisation invalide."));

        if (utilisateur.getResetTokenExpiry() == null ||
                utilisateur.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResetTokenInvalidException("Le token a expiré.");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateur.setResetToken(null);
        utilisateur.setResetTokenExpiry(null);
        utilisateurRepository.save(utilisateur);
    }
}
