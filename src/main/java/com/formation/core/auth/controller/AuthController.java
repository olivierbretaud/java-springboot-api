package com.formation.core.auth.controller;

import com.formation.core.auth.dto.AuthResponse;
import com.formation.core.auth.dto.ChangePasswordRequest;
import com.formation.core.auth.dto.ChangePasswordResponse;
import com.formation.core.auth.dto.LoginRequest;
import com.formation.core.auth.dto.ProfileResponse;
import com.formation.core.auth.dto.RefreshRequest;
import com.formation.core.auth.dto.ResetPasswordRequest;
import com.formation.core.auth.dto.ResetPasswordResponse;
import com.formation.core.auth.service.AuthService;
import com.formation.engine.entities.UtilisateurEntity;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new ResetPasswordResponse(
            "Si un compte est associé à cet email, un lien de réinitialisation vous a été envoyé."
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(new ChangePasswordResponse(
            "Votre mot de passe a été réinitialisé avec succès. Vous pouvez maintenant vous connecter."
        ));
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProfileResponse> profile(@AuthenticationPrincipal UtilisateurEntity utilisateur) {
        return ResponseEntity.ok(new ProfileResponse(
            utilisateur.getId(),
            utilisateur.getEmail(),
            utilisateur.getNom(),
            utilisateur.getPrenom(),
            utilisateur.getRole().getCode()
        ));
    }
}
