# Concepts Fondamentaux en Détail

## Table des matières

1. [Entity vs DTO](#entity-vs-dto)
2. [Repository Pattern](#repository-pattern)
3. [Injection de dépendances](#injection-de-dépendances)
4. [Transactional](#transactional)
5. [Validation Bean](#validation-bean)
6. [Gestion des erreurs](#gestion-des-erreurs)
7. [HTTP Status Codes](#http-status-codes)

---

## Entity vs DTO

### Différence fondamentale

| Aspect | Entity | DTO |
|--------|--------|-----|
| **Rôle** | Représente la table BD | Communique les données API |
| **Persistance** | Persistée en BD | Non persistée |
| **Champs** | Tous les champs internes | Uniquement publics |
| **Annotations** | JPA (@Entity, @Column) | Validation (Bean Validation) |
| **Exemple** | `Produit.java` | `ProduitDTO.java` |

### Pourquoi deux classes ?

#### Problème sans DTO
```java
@Entity
public class Produit {
    private Long id;
    private String nom;
    private String motDePasseAdmin;  // ⚠️ Ne pas exposer !
    private String codePromo;         // ⚠️ Privé !
}

@RestController
public class ProduitController {
    @GetMapping("/{id}")
    public Produit getProduit(@PathVariable Long id) {
        return repository.findById(id);  // ⚠️ Expose TOUT
    }
}
```

**Problème** : L'API retourne `motDePasseAdmin` et `codePromo` au client !

#### Solution avec DTO
```java
@Entity
public class Produit {
    private Long id;
    private String nom;
    private String motDePasseAdmin;  // ✅ Non exposé
    private String codePromo;         // ✅ Non exposé
}

public class ProduitDTO {
    private Long id;
    private String nom;
    // Seulement les champs publics !
}

@RestController
public class ProduitController {
    @GetMapping("/{id}")
    public ProduitDTO getProduit(@PathVariable Long id) {
        Produit entity = repository.findById(id);
        return mapper.toDTO(entity);  // ✅ Retourne seulement les champs publics
    }
}
```

### Exemple concret

**Entity** : Représente la BD

```java
@Entity
@Table(name = "utilisateurs")
@Data
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String email;
    private String passwordHash;      // Champ privé !
    private String codeActivation;    // Champ privé !
    private LocalDateTime dateCreation;
    private Boolean isAdmin;          // Champ privé !
}
```

**DTO** : Modèle API

```java
@Data
public class UtilisateurDTO {
    private Long id;
    
    @Email
    private String email;
    
    private LocalDateTime dateCreation;
    // PAS de passwordHash, codeActivation, isAdmin
}
```

---

## Repository Pattern

### Concept

Le **Repository Pattern** cache l'implémentation de l'accès aux données derrière une interface.

```
Client → Interface Repository → Implémentation (JPA/SQL) → BD
```

### Avantages

1. **Abstraction** : Le client ne connaît pas les détails de BD
2. **Testabilité** : Facile de mock un repository
3. **Flexibilité** : Changer d'implémentation BD est simple
4. **Maintenabilité** : Logique de requête centralisée

### Exemple complet

#### 1. Interface Repository

```java
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    // JpaRepository fournit automatiquement :
    // - save(), findById(), findAll(), delete(), etc.
    
    // Méthodes personnalisées :
    List<Produit> findByPrixGreaterThan(BigDecimal prix);
    List<Produit> findByNomContainingIgnoreCase(String nom);
}
```

#### 2. Utilisation dans le Service

```java
@Service
public class ProduitService {
    private final ProduitRepository repository;  // Injecter l'interface
    
    public List<Produit> findExpensive() {
        return repository.findByPrixGreaterThan(new BigDecimal("1000"));
        // On n'écrit pas la requête SQL !
    }
}
```

#### 3. Test avec Mock

```java
@Test
public void testFindExpensive() {
    // Créer un mock du repository
    ProduitRepository mockRepo = mock(ProduitRepository.class);
    
    // Configurer le comportement
    List<Produit> expected = List.of(new Produit(/* ... */));
    when(mockRepo.findByPrixGreaterThan(any())).thenReturn(expected);
    
    // Tester le service
    ProduitService service = new ProduitService(mockRepo);
    List<Produit> result = service.findExpensive();
    
    assert result.equals(expected);
}
```

---

## Injection de dépendances

### Concept

Spring **injecte automatiquement** les objets nécessaires, sans code boilerplate.

### Méthode 1 : @Autowired

```java
@Service
public class ProduitService {
    
    @Autowired  // ← Spring injecte automatiquement
    private ProduitRepository repository;
    
    public List<Produit> findAll() {
        return repository.findAll();
    }
}
```

**Problèmes** :
- ⚠️ Peut être null si injection échoue
- ⚠️ Difficile à tester
- ⚠️ Moins explicite

### Méthode 2 : Constructor Injection (✅ Recommandée)

```java
@Service
public class ProduitService {
    
    private final ProduitRepository repository;  // final !
    
    // Constructor
    public ProduitService(ProduitRepository repository) {
        this.repository = repository;
    }
    
    public List<Produit> findAll() {
        return repository.findAll();
    }
}
```

**Avantages** :
- ✅ Explicite
- ✅ Facilite les tests
- ✅ Immuable (final)
- ✅ Compile-time safe

### Méthode 3 : @RequiredArgsConstructor (✅ + Lombok)

```java
@Service
@RequiredArgsConstructor  // ← Lombok génère le constructor
public class ProduitService {
    
    private final ProduitRepository repository;
    
    // Constructor généré automatiquement :
    // public ProduitService(ProduitRepository repository) {
    //     this.repository = repository;
    // }
    
    public List<Produit> findAll() {
        return repository.findAll();
    }
}
```

**Avantages** :
- ✅ Moins de code
- ✅ Facile à lire
- ✅ Même avantages que constructor injection

### Utilisation dans les tests

```java
@Test
public void testFindAll() {
    // Créer les mocks
    ProduitRepository mockRepo = mock(ProduitRepository.class);
    List<Produit> expected = List.of(new Produit(/* ... */));
    when(mockRepo.findAll()).thenReturn(expected);
    
    // Injecter via constructor
    ProduitService service = new ProduitService(mockRepo);
    
    // Tester
    List<Produit> result = service.findAll();
    assert result.equals(expected);
}
```

---

## @Transactional

### Concept

`@Transactional` gère **automatiquement** les transactions BD.

```java
@Service
public class ProduitService {
    
    @Transactional
    public void updateAndNotify(Long id, String nom) {
        // BEGIN TRANSACTION
        
        Produit produit = repository.findById(id);
        produit.setNom(nom);
        repository.save(produit);  // UPDATE dans la BD
        
        notificationService.send("Produit mis à jour");
        
        // Si exception : ROLLBACK automatique
        // Sinon : COMMIT automatique
    }
}
```

### Flux détaillé

#### Succès (COMMIT)

```
BEGIN TRANSACTION
    ↓
UPDATE produit SET nom = ...
    ↓
INSERT INTO notifications ...
    ↓
COMMIT  ← Les deux modifications sont persistées
```

#### Erreur (ROLLBACK)

```
BEGIN TRANSACTION
    ↓
UPDATE produit SET nom = ...
    ↓
notificationService.send() → Exception !
    ↓
ROLLBACK  ← TOUT est annulé (y compris l'UPDATE)
    ↓
Exception propagée au client
```

### Isolation des transactions

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Niveau d'isolation (du moins au plus strict)
        connection.isolation: 2  # READ_COMMITTED (défaut)
        # 1 = READ_UNCOMMITTED (lectures sales)
        # 2 = READ_COMMITTED (défaut)
        # 4 = REPEATABLE_READ
        # 8 = SERIALIZABLE (plus strict, plus lent)
```

---

## Validation Bean

### Annotations principales

#### @NotNull vs @NotBlank

```java
@NotNull        // null → erreur
private String nom;

@NotBlank       // null, vide, espaces → erreur
private String nom;

// Exemples :
nom = null          // @NotNull ✓, @NotBlank ✗
nom = ""            // @NotNull ✓, @NotBlank ✗
nom = "   "         // @NotNull ✓, @NotBlank ✗
nom = "Alice"       // @NotNull ✓, @NotBlank ✓
```

#### @Size, @Min, @Max

```java
@Size(min = 3, max = 50)           // Longueur de chaîne
private String nom;

@Min(0)                            // Minimum
@Max(100)                          // Maximum
private Integer age;

@Positive                          // > 0
@PositiveOrZero                    // >= 0
private Integer quantite;

@DecimalMin(value = "0.01")        // Minimum pour BigDecimal
@DecimalMax(value = "9999.99")     // Maximum pour BigDecimal
private BigDecimal prix;
```

#### @Email, @Pattern

```java
@Email                             // Format email
private String email;

@Pattern(regexp = "^[A-Z].*")      // Expression régulière
private String nom;
```

### Utilisation dans le Controller

```java
@PostMapping
public ResponseEntity<ProduitDTO> create(
        @Valid @RequestBody ProduitDTO dto  // ← @Valid active la validation
) {
    // Si validation échoue :
    // - Spring génère une exception MethodArgumentNotValidException
    // - Retourne 400 Bad Request avec les erreurs
    
    // Si validation réussit :
    // - execute le code
    
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
}
```

### Récupérer les erreurs de validation

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

---

## Gestion des erreurs

### Exception personnalisée

```java
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
```

### Lever l'exception

```java
@Service
public class ProduitService {
    
    public ProduitDTO findById(Long id) {
        return repository.findById(id)
            .map(this::convertToDTO)
            .orElseThrow(() -> new EntityNotFoundException(
                "Produit avec ID " + id + " non trouvé"
            ));
    }
}
```

### Capturer et répondre

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(Exception.class)  // Attrape TOUTES les autres exceptions
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Erreur serveur");
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}
```

### Classe de réponse d'erreur

```java
@Data
public class ErrorResponse {
    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String path;  // URL qui a causé l'erreur
    
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
```

---

## HTTP Status Codes

### 2xx Succès

| Code | Nom | Utilité |
|------|-----|---------|
| 200 | OK | Requête réussie, données retournées |
| 201 | Created | Ressource créée (POST) |
| 202 | Accepted | Requête acceptée (traitement asynchrone) |
| 204 | No Content | Succès mais aucun contenu (DELETE) |

### 4xx Erreur du client

| Code | Nom | Utilité |
|------|-----|---------|
| 400 | Bad Request | Données invalides (validation échouée) |
| 401 | Unauthorized | Non authentifié |
| 403 | Forbidden | Non autorisé |
| 404 | Not Found | Ressource non trouvée |
| 409 | Conflict | Conflit (ex: doublon) |
| 429 | Too Many Requests | Trop de requêtes (rate limit) |

### 5xx Erreur du serveur

| Code | Nom | Utilité |
|------|-----|---------|
| 500 | Internal Server Error | Erreur serveur générique |
| 502 | Bad Gateway | Problème avec le serveur upstream |
| 503 | Service Unavailable | Serveur surchargé ou maintenance |

### Utilisation dans Spring

```java
@GetMapping("/{id}")
public ResponseEntity<ProduitDTO> getProduit(@PathVariable Long id) {
    // Trouvé
    if (produit != null) {
        return ResponseEntity.ok(produit);  // 200 OK
    }
    
    // Non trouvé
    return ResponseEntity.notFound().build();  // 404 Not Found
}

@PostMapping
public ResponseEntity<ProduitDTO> create(@Valid @RequestBody ProduitDTO dto) {
    ProduitDTO created = service.create(dto);
    
    // Retourner 201 Created
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(created);
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    
    // Pas de contenu
    return ResponseEntity.noContent().build();  // 204 No Content
}
```

---

**Version** : 1.0  
**Dernière mise à jour** : 2026-09-16
