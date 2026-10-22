package com.formation.engine.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Validation sur @RequestParam / @PathVariable
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().iterator().next().getMessage();
        return error(400, "Paramètre invalide", message);
    }

    // Validation sur @RequestBody avec @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(400, "Données invalides", message);
    }

    // Mauvais type de paramètre (ex: String à la place d'un Long)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "inconnu";
        String message = "Le paramètre '" + ex.getName() + "' doit être de type " + typeName;
        return error(400, "Type invalide", message);
    }

    // Accès refusé (méthode @PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return error(403, "Accès refusé", "Vous n'avez pas les droits nécessaires pour effectuer cette action.");
    }

    // Email ou mot de passe incorrect
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return error(401, "Authentification échouée", "Email ou mot de passe incorrect.");
    }

    // Token de réinitialisation invalide ou expiré
    @ExceptionHandler(ResetTokenInvalidException.class)
    public ResponseEntity<Map<String, Object>> handleResetTokenInvalid(ResetTokenInvalidException ex) {
        return error(400, "Token invalide", ex.getMessage());
    }

    // Opération métier impossible dans l'état actuel
    @ExceptionHandler(CommandeNonSupprimableException.class)
    public ResponseEntity<Map<String, Object>> handleCommandeNonSupprimable(CommandeNonSupprimableException ex) {
        return error(409, "Opération impossible", ex.getMessage());
    }

    // Ressource introuvable
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return error(404, "Ressource introuvable", ex.getMessage());
    }

    // Erreurs base de données (SQL, JDBC, JPA)
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        log.error("Erreur base de données", ex);
        return error(500, "Erreur base de données", "Une erreur est survenue lors de l'accès aux données.");
    }

    // Toutes les autres erreurs non gérées
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Erreur inattendue [{}] : {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return error(500, "Erreur interne", "Une erreur inattendue est survenue.");
    }

    private ResponseEntity<Map<String, Object>> error(int status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "status", status,
            "error", error,
            "message", message,
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
