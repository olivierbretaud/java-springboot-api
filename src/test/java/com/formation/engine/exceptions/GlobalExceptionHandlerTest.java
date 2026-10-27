package com.formation.engine.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
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

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Paramètre invalide");
        assertThat(response.getBody().getDetail()).isEqualTo("doit être positif");
    }

    @Test
    void handleMethodArgumentNotValid_retourne_400_avec_champs_invalides() {
        FieldError fieldError = new FieldError("produit", "nom", "ne doit pas être vide");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Données invalides");
        assertThat(response.getBody().getDetail()).contains("nom : ne doit pas être vide");
    }

    @Test
    void handleMethodArgumentNotValid_retourne_400_avec_plusieurs_champs() {
        FieldError erreurNom = new FieldError("produit", "nom", "ne doit pas être vide");
        FieldError erreurPrix = new FieldError("produit", "prix", "doit être positif");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(erreurNom, erreurPrix));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getDetail())
            .contains("nom : ne doit pas être vide")
            .contains("prix : doit être positif");
    }

    @Test
    void handleTypeMismatch_retourne_400_avec_type_attendu() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        doReturn(Long.class).when(ex).getRequiredType();

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Type invalide");
        assertThat(response.getBody().getDetail()).contains("'id'").contains("Long");
    }

    @Test
    void handleTypeIllegalArgumentException_retourne_409_avec_message() {
        IllegalArgumentException ex = mock(IllegalArgumentException.class);
        when(ex.getMessage()).thenReturn("Conflit d'email");

        ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getTitle()).isEqualTo("Conflit");
    }

    @Test
    void handleEntityNotFound_retourne_404_avec_message() {
        EntityNotFoundException ex = new EntityNotFoundException("Catégorie introuvable : 99");

        ResponseEntity<ProblemDetail> response = handler.handleEntityNotFound(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Ressource introuvable");
        assertThat(response.getBody().getDetail()).isEqualTo("Catégorie introuvable : 99");
    }

    @Test
    void handleDataAccess_retourne_500_avec_message_generique() {
        DataAccessException ex = new DataIntegrityViolationException(
            "JDBC exception executing SQL [select ...] [ERROR: relation \"produits\" does not exist]"
        );

        ResponseEntity<ProblemDetail> response = handler.handleDataAccess(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getTitle()).isEqualTo("Erreur base de données");
        assertThat(response.getBody().getDetail()).isEqualTo("Une erreur est survenue lors de l'accès aux données.");
        assertThat(response.getBody().getDetail()).doesNotContain("select");
    }

    @Test
    void handleGeneric_retourne_500_avec_message_generique() {
        Exception ex = new RuntimeException("connexion base de données échouée");

        ResponseEntity<ProblemDetail> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getTitle()).isEqualTo("Erreur interne");
        assertThat(response.getBody().getDetail()).isEqualTo("Une erreur inattendue est survenue.");
    }

    @Test
    void handleGeneric_retourne_500_avec_exception_checked() {
        Exception ex = new IllegalStateException("état incohérent détecté");

        ResponseEntity<ProblemDetail> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getTitle()).isEqualTo("Erreur interne");
        assertThat(response.getBody().getDetail()).isEqualTo("Une erreur inattendue est survenue.");
    }

    @Test
    void handleBadCredentials_retourne_401_avec_message_generique() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ProblemDetail> response = handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().getTitle()).isEqualTo("Authentification échouée");
        assertThat(response.getBody().getDetail()).isEqualTo("Email ou mot de passe incorrect.");
        assertThat(response.getBody().getDetail()).doesNotContain("Bad credentials");
    }
}
