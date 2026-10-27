package com.formation.engine.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Validation sur @RequestParam / @PathVariable
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().iterator().next().getMessage();
        return problem(HttpStatus.BAD_REQUEST, "Paramètre invalide", detail);
    }

    // Validation sur @RequestBody avec @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return problem(HttpStatus.BAD_REQUEST, "Données invalides", detail);
    }

    // Mauvais type de paramètre (ex: String à la place d'un Long)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "inconnu";
        String detail = "Le paramètre '" + ex.getName() + "' doit être de type " + typeName;
        return problem(HttpStatus.BAD_REQUEST, "Type invalide", detail);
    }

    // Champ de tri inexistant sur l'entité (ex: sort=toto)
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handlePropertyReference(PropertyReferenceException ex) {
        String detail = "Champ de tri invalide : '" + ex.getPropertyName() + "'";
        return problem(HttpStatus.BAD_REQUEST, "Paramètre de tri invalide", detail);
    }

    // Accès refusé (méthode @PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Accès refusé", "Vous n'avez pas les droits nécessaires pour effectuer cette action.");
    }

    // Email ou mot de passe incorrect
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentification échouée", "Email ou mot de passe incorrect.");
    }

    // Token de réinitialisation invalide ou expiré
    @ExceptionHandler(ResetTokenInvalidException.class)
    public ResponseEntity<ProblemDetail> handleResetTokenInvalid(ResetTokenInvalidException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Token invalide", ex.getMessage());
    }

    // Opération métier impossible dans l'état actuel
    @ExceptionHandler(CommandeNonSupprimableException.class)
    public ResponseEntity<ProblemDetail> handleCommandeNonSupprimable(CommandeNonSupprimableException ex) {
        return problem(HttpStatus.CONFLICT, "Opération impossible", ex.getMessage());
    }

    // Conflit métier (ex: email déjà utilisé)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
    }

    // Ressource introuvable
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
    }

    // Erreurs base de données (SQL, JDBC, JPA)
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException ex) {
        log.error("Erreur base de données", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur base de données", "Une erreur est survenue lors de l'accès aux données.");
    }

    // Toutes les autres erreurs non gérées
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Erreur inattendue [{}] : {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", "Une erreur inattendue est survenue.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("about:blank"));
        return ResponseEntity.status(status).body(pd);
    }
}
