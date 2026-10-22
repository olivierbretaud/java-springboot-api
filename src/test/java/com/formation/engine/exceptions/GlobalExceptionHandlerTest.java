package com.formation.engine.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleConstraintViolation_retourne_400_avec_message() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("doit être positif");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "Paramètre invalide");
        assertThat(response.getBody()).containsEntry("message", "doit être positif");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleMethodArgumentNotValid_retourne_400_avec_champs_invalides() {
        FieldError fieldError = new FieldError("produit", "nom", "ne doit pas être vide");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "Données invalides");
        assertThat(response.getBody().get("message").toString()).contains("nom : ne doit pas être vide");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleMethodArgumentNotValid_retourne_400_avec_plusieurs_champs() {
        FieldError erreurNom = new FieldError("produit", "nom", "ne doit pas être vide");
        FieldError erreurPrix = new FieldError("produit", "prix", "doit être positif");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(erreurNom, erreurPrix));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        String message = response.getBody().get("message").toString();
        assertThat(message).contains("nom : ne doit pas être vide");
        assertThat(message).contains("prix : doit être positif");
    }

    @Test
    void handleTypeMismatch_retourne_400_avec_type_attendu() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getRequiredType()).thenReturn((Class) Long.class);

        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "Type invalide");
        assertThat(response.getBody().get("message").toString()).contains("'id'");
        assertThat(response.getBody().get("message").toString()).contains("Long");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleEntityNotFound_retourne_404_avec_message() {
        EntityNotFoundException ex = new EntityNotFoundException("Catégorie introuvable : 99");

        ResponseEntity<Map<String, Object>> response = handler.handleEntityNotFound(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("error", "Ressource introuvable");
        assertThat(response.getBody()).containsEntry("message", "Catégorie introuvable : 99");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleDataAccess_retourne_500_avec_message_generique() {
        DataAccessException ex = new DataIntegrityViolationException(
            "JDBC exception executing SQL [select ...] [ERROR: relation \"produits\" does not exist]"
        );

        ResponseEntity<Map<String, Object>> response = handler.handleDataAccess(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Erreur base de données");
        assertThat(response.getBody()).containsEntry("message", "Une erreur est survenue lors de l'accès aux données.");
        assertThat(response.getBody()).doesNotContainValue("select");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleGeneric_retourne_500_avec_message_generique() {
        Exception ex = new RuntimeException("connexion base de données échouée");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Erreur interne");
        assertThat(response.getBody()).containsEntry("message", "Une erreur inattendue est survenue.");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleGeneric_retourne_500_avec_exception_checked() {
        Exception ex = new IllegalStateException("état incohérent détecté");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Erreur interne");
        assertThat(response.getBody()).containsEntry("message", "Une erreur inattendue est survenue.");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleBadCredentials_retourne_401_avec_message_generique() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).containsEntry("error", "Authentification échouée");
        assertThat(response.getBody()).containsEntry("message", "Email ou mot de passe incorrect.");
        assertThat(response.getBody()).doesNotContainValue("Bad credentials");
        assertThat(response.getBody()).containsKey("timestamp");
    }
}
