# ÉTAPE 6 : Gestion des erreurs & validation

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Concepts clés](#concepts-clés)
3. [Validation des entrées (@Valid + Bean Validation)](#validation-des-entrées)
4. [Exceptions métier custom](#exceptions-métier-custom)
5. [Réponses d'erreur standardisées (ProblemDetail RFC 9457)](#problemdetail-rfc-9457)
6. [Gestion centralisée (@ControllerAdvice)](#controlleradvice)
7. [Fichiers implémentés](#fichiers-implémentés)
8. [Pièges courants](#pièges-courants)
9. [Récapitulatif](#récapitulatif)

---

## 🎯 Objectif

**Retourner des erreurs claires, cohérentes et sécurisées à chaque couche de l'application.**

Après cette étape, vous allez pouvoir :
- ✅ Valider les données entrantes avec `@Valid` et les annotations Bean Validation
- ✅ Retourner des erreurs au format standard RFC 9457 (`ProblemDetail`)
- ✅ Centraliser tous les `@ExceptionHandler` dans un seul `@RestControllerAdvice`
- ✅ Distinguer erreurs métier (404, 409) et erreurs techniques (500)
- ✅ Comprendre pourquoi ne jamais exposer la stacktrace en production

---

## 📖 Concepts clés

### Pourquoi standardiser les erreurs ?

Sans standardisation, chaque endpoint retourne un format différent :

```json
// Endpoint A
{ "msg": "not found" }

// Endpoint B
{ "error": true, "code": 404, "description": "..." }

// Endpoint C (Spring par défaut sans config)
{ "timestamp": "...", "status": 404, "error": "Not Found", "path": "..." }
```

Le client frontend ou le consommateur API ne peut pas écrire de code générique pour gérer ces erreurs. Un format unique résout ce problème.

### La pyramide des codes HTTP

| Code | Signification | Exemple |
|------|--------------|---------|
| 400 | Données invalides envoyées par le client | Champ manquant, type incorrect |
| 401 | Non authentifié | Token absent ou expiré |
| 403 | Authentifié mais non autorisé | Rôle insuffisant |
| 404 | Ressource introuvable | `GET /produits/999` |
| 409 | Conflit métier | Email déjà utilisé, statut incompatible |
| 422 | Entité non traitable (variante de 400) | Rarement utilisé avec Bean Validation |
| 500 | Erreur interne — bug du serveur | NullPointerException non gérée |

### Bean Validation vs validation métier

| Type | Où | Exemple |
|------|----|---------|
| Bean Validation | DTO / `@RequestBody` | `@NotBlank`, `@Min`, `@Email` |
| Validation métier | Service | "Un produit avec ce nom existe déjà" |

Bean Validation s'arrête au format ; la validation métier vérifie les règles du domaine.

---

## ✅ Validation des entrées

### Annotations disponibles

```java
public class ProduitCreatePayload {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être positif")
    private BigDecimal prix;

    @Min(value = 1, message = "L'unité par packaging doit être au moins 1")
    private Integer uniteParPackaging;

    @NotNull(message = "La catégorie est obligatoire")
    private Long idCategorie;
}
```

### Déclencher la validation dans le controller

```java
@PostMapping
public ResponseEntity<ProduitResponse> creer(@Valid @RequestBody ProduitCreatePayload payload) {
    // ...
}
```

**`@Valid` est obligatoire.** Sans lui, les annotations sur le DTO sont déclarées mais jamais évaluées — aucune erreur n'est levée.

### Valider une liste imbriquée

```java
public class CommandeCreatePayload {

    @NotEmpty(message = "La commande doit contenir au moins une ligne")
    @Valid  // propage la validation sur chaque élément de la liste
    private List<LigneCommandePayload> lignes;
}
```

`@Valid` sur la liste déclenche la validation de chaque `LigneCommandePayload`. Sans lui, seul le `@NotEmpty` sur la liste elle-même est vérifié.

---

## 💥 Exceptions métier custom

Pour les cas métier non couverts par les exceptions standard, on crée des classes dédiées.

### `CommandeNonSupprimableException`

```java
public class CommandeNonSupprimableException extends RuntimeException {

    public CommandeNonSupprimableException(Long id, String statut) {
        super("La commande " + id + " ne peut pas être supprimée (statut : " + statut + ")");
    }
}
```

Levée dans le service :
```java
if (!commande.getStatut().equals(StatutCommande.EN_ATTENTE)) {
    throw new CommandeNonSupprimableException(id, commande.getStatut().name());
}
```

Gérée dans le handler → 409 :
```java
@ExceptionHandler(CommandeNonSupprimableException.class)
public ResponseEntity<ProblemDetail> handleCommandeNonSupprimable(CommandeNonSupprimableException ex) {
    return problem(HttpStatus.CONFLICT, "Opération impossible", ex.getMessage());
}
```

### `ResetTokenInvalidException`

```java
public class ResetTokenInvalidException extends RuntimeException {
    public ResetTokenInvalidException(String message) {
        super(message);
    }
}
```

Gérée dans le handler → 400.

### Pourquoi des exceptions custom plutôt qu'un code de retour ?

```java
// ❌ Le service retourne null ou un boolean — le controller doit interpréter
if (!commandeService.supprimer(id)) {
    return ResponseEntity.status(409).build();
}

// ✅ Le service lève une exception — le handler la capte automatiquement
commandeService.supprimer(id); // lève CommandeNonSupprimableException si impossible
```

La deuxième approche garde le controller et le service simples ; toute la logique de mapping vers HTTP est dans le handler.

---

## 📐 ProblemDetail RFC 9457

### Pourquoi RFC 9457 ?

C'est le standard IETF pour les réponses d'erreur HTTP. Spring Boot 3+ le supporte nativement. Un client qui connaît le standard peut traiter les erreurs de n'importe quelle API compatible.

### Activer le support Spring Boot — `application.yml`

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

Cette propriété active la gestion automatique des erreurs Spring (404, 405, 415…) au format ProblemDetail. Nos handlers custom utilisent déjà `ProblemDetail` directement.

### Structure d'une réponse ProblemDetail

```json
{
  "type": "about:blank",
  "title": "Ressource introuvable",
  "status": 404,
  "detail": "Produit 42 introuvable",
  "instance": "/api/produits/42"
}
```

| Champ | Rôle |
|-------|------|
| `type` | URI décrivant le type d'erreur (par convention `about:blank` si non documenté) |
| `title` | Libellé court et stable — ne change pas entre appels |
| `status` | Code HTTP |
| `detail` | Message humain — peut changer, contient le contexte de l'erreur |
| `instance` | URL de la requête qui a provoqué l'erreur |

### Avant / Après la migration

**Avant (Map custom) :**
```json
{
  "status": 404,
  "error": "Ressource introuvable",
  "message": "Produit 42 introuvable",
  "timestamp": "2026-10-27T14:32:11.456"
}
```

**Après (ProblemDetail RFC 9457) :**
```json
{
  "type": "about:blank",
  "title": "Ressource introuvable",
  "status": 404,
  "detail": "Produit 42 introuvable"
}
```

Le format est désormais interopérable avec tout client qui respecte la RFC.

---

## 🎯 @ControllerAdvice

### Principe

Sans `@RestControllerAdvice`, chaque controller devrait gérer ses propres exceptions — du code dupliqué, des formats inconsistants. Un seul `@RestControllerAdvice` centralise tout :

```
Controller A  ──┐
Controller B  ──┼──► GlobalExceptionHandler ──► réponse ProblemDetail uniforme
Controller C  ──┘
```

### Le handler complet — `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 — @RequestParam / @PathVariable
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().iterator().next().getMessage();
        return problem(HttpStatus.BAD_REQUEST, "Paramètre invalide", detail);
    }

    // 400 — @RequestBody avec @Valid (liste les champs invalides)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return problem(HttpStatus.BAD_REQUEST, "Données invalides", detail);
    }

    // 400 — String à la place d'un Long dans l'URL
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "Le paramètre '" + ex.getName() + "' doit être de type "
                + ex.getRequiredType().getSimpleName();
        return problem(HttpStatus.BAD_REQUEST, "Type invalide", detail);
    }

    // 401 — email ou mot de passe incorrect
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentification échouée", "Email ou mot de passe incorrect.");
    }

    // 403 — @PreAuthorize échoue
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Accès refusé", "Vous n'avez pas les droits nécessaires.");
    }

    // 404 — EntityNotFoundException
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
    }

    // 409 — conflit métier (email déjà utilisé…)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
    }

    // 500 — erreur base de données (le détail SQL ne doit jamais atteindre le client)
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException ex) {
        log.error("Erreur base de données", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur base de données",
                "Une erreur est survenue lors de l'accès aux données.");
    }

    // 500 — filet de sécurité pour tout le reste
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
```

---

## 💻 Fichiers implémentés

### `GlobalExceptionHandler.java`

Handler central : 10 cas couverts, retourne `ProblemDetail` pour chacun.

### `CommandeNonSupprimableException.java`

Exception levée quand une commande ne peut pas être supprimée selon son statut.

### `ResetTokenInvalidException.java`

Exception levée quand un token de réinitialisation de mot de passe est invalide ou expiré.

### DTOs validés

| DTO | Contraintes |
|-----|------------|
| `ProduitCreatePayload` | `@NotBlank`, `@Size`, `@NotNull`, `@DecimalMin`, `@Min` |
| `CommandeCreatePayload` | `@NotEmpty`, `@Valid` (cascade sur lignes) |
| `LigneCommandePayload` | `@NotNull`, `@Min` |
| `LoginRequest` | `@Email`, `@NotBlank` |
| `UtilisateurCreatePayload` | `@NotBlank`, `@Email`, `@Size` |
| `ChangePasswordRequest` | `@NotBlank`, `@Size` |

### `application.yml` — ajout

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

---

## 💡 Pièges courants

### `@Valid` manquant — validation silencieusement ignorée

```java
// ❌ Les annotations @NotBlank sur le DTO ne sont jamais évaluées
@PostMapping
public ResponseEntity<ProduitResponse> creer(@RequestBody ProduitCreatePayload payload) { ... }

// ✅
@PostMapping
public ResponseEntity<ProduitResponse> creer(@Valid @RequestBody ProduitCreatePayload payload) { ... }
```

Aucune erreur de compilation, aucun avertissement — Spring accepte les données invalides en silence.

### Exposer la stacktrace en production

```java
// ❌ Le client reçoit le message d'exception interne — fuite d'information
@ExceptionHandler(Exception.class)
public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur", ex.getMessage()); // ex.getMessage() peut contenir un chemin de fichier, un nom de classe, une requête SQL...
}

// ✅ Message générique côté client, stacktrace dans les logs serveur uniquement
@ExceptionHandler(Exception.class)
public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
    log.error("Erreur inattendue", ex);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", "Une erreur inattendue est survenue.");
}
```

### Ordre des handlers — le plus spécifique d'abord

```java
// ❌ Exception.class capture CommandeNonSupprimableException avant son handler dédié
@ExceptionHandler(Exception.class)           // → 500 pour tout
@ExceptionHandler(CommandeNonSupprimableException.class)  // jamais atteint

// ✅ Spring choisit le handler le plus spécifique automatiquement
//    Mais si deux handlers couvrent la même exception, l'ordre de déclaration compte
@ExceptionHandler(CommandeNonSupprimableException.class)  // → 409
@ExceptionHandler(Exception.class)           // → 500 pour tout le reste
```

En pratique, Spring résout l'exception la plus proche dans la hiérarchie — mais déclarer le plus spécifique en premier reste une bonne pratique de lisibilité.

### `@Valid` vs `@Validated`

```java
// @Valid — standard Jakarta, suffit pour la validation de base sur @RequestBody
@PostMapping
public void creer(@Valid @RequestBody ProduitCreatePayload payload) { ... }

// @Validated — Spring, nécessaire pour valider les @RequestParam / @PathVariable
@GetMapping
public Page<ProduitResponse> lister(@RequestParam @Min(0) int page) { ... }
// Le controller doit être annoté @Validated au niveau classe pour que @Min soit évalué
```

---

## ✅ Récapitulatif

```
Validation des entrées
├── @Valid sur @RequestBody          → déclenche Bean Validation sur le DTO
├── @NotBlank, @Size, @Min, @Email   → contraintes déclaratives sur les champs
├── @NotEmpty + @Valid sur List<>    → cascade la validation sur les éléments
└── ConstraintViolationException     → levée automatiquement par Spring si @Valid échoue

ProblemDetail RFC 9457
├── spring.mvc.problemdetails.enabled: true  → active le format sur les erreurs Spring natives
├── ProblemDetail.forStatusAndDetail(...)    → factory method Spring Boot 3+
├── setTitle() / setDetail() / setType()     → champs standardisés
└── Interopérable avec tout client RFC 9457

@RestControllerAdvice
├── @ExceptionHandler par type d'exception   → un handler par cas
├── 400 (validation), 401, 403, 404, 409, 500
├── log.error() sur les 500                  → stacktrace dans les logs, pas dans la réponse
└── Exceptions custom (CommandeNonSupprimable, ResetTokenInvalid) → 409 / 400

Sécurité
└── Ne jamais exposer ex.getMessage() dans un 500 → fuite d'information interne
```

---

*Dernière mise à jour : 2026-10-27*
