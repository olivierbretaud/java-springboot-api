# Architecture de l'application

## Table des matières

1. [Principes architecturaux](#principes-architecturaux)
2. [Couches de l'application](#couches-de-lapplication)
3. [Patterns utilisés](#patterns-utilisés)
4. [Configuration Spring Boot](#configuration-spring-boot)
5. [Gestion des erreurs](#gestion-des-erreurs)

---

## Principes architecturaux

### Architecture en couches

L'application suit une architecture classique en couches séparant les responsabilités :

```
┌─────────────────────────────────────────────┐
│          REST API Layer                      │
│  (Controllers - @RestController)             │
│  Responsabilité : Gérer les requêtes HTTP   │
└─────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────┐
│       Business Logic Layer                   │
│  (Services - @Service)                       │
│  Responsabilité : Logique métier             │
└─────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────┐
│     Data Access Layer                        │
│  (Repositories - extends JpaRepository)      │
│  Responsabilité : Accès aux données          │
└─────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────┐
│      Database Layer                          │
│  (PostgreSQL via Hibernate/JPA)              │
│  Responsabilité : Persister les données      │
└─────────────────────────────────────────────┘
```

### Avantages de cette architecture

✅ **Séparation des responsabilités** : Chaque couche a un rôle précis  
✅ **Testabilité** : Facile à tester chaque couche indépendamment  
✅ **Maintenabilité** : Modifications localisées et prévisibles  
✅ **Réutilisabilité** : Services utilisables par plusieurs controllers  
✅ **Scalabilité** : Facile d'ajouter de nouvelles fonctionnalités  

---

## Couches de l'application

### 1. Layer - Controller (REST API)

**Responsabilité** : Gérer les requêtes HTTP et les réponses

**Annotations clés** :
- `@RestController` : Indique que la classe gère les endpoints
- `@RequestMapping` : Préfixe pour tous les endpoints
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` : Méthodes HTTP
- `@PathVariable` : Paramètre dans l'URL
- `@RequestParam` : Paramètre dans la query string
- `@RequestBody` : Récupère le JSON du corps de la requête
- `@Valid` : Valide automatiquement avec les annotations Bean Validation

**Exemple** :
```java
@RestController
@RequestMapping("/api/produits")
public class ProduitController {
    
    private final ProduitService service;
    
    public ProduitController(ProduitService service) {
        this.service = service;
    }
    
    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getAllProduits() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProduitDTO> getProduitById(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<ProduitDTO> createProduit(@Valid @RequestBody ProduitDTO dto) {
        ProduitDTO created = service.create(dto);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ProduitDTO> updateProduit(
            @PathVariable Long id,
            @Valid @RequestBody ProduitDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Responsabilités du Controller** :
- Extraire les données de la requête
- Valider (via @Valid)
- Appeler le Service
- Retourner une ResponseEntity avec le bon statut HTTP

### 2. Service Layer (Logique métier)

**Responsabilité** : Implémenter la logique métier, convertir Entity ↔ DTO

**Annotations clés** :
- `@Service` : Indique que c'est un Service managé par Spring
- `@Transactional` : Gère les transactions BD
- `@Autowired` ou Constructor injection : Injection de dépendances
- `@RequiredArgsConstructor` (Lombok) : Génère le constructor

**Exemple** :
```java
@Service
@RequiredArgsConstructor
public class ProduitService {
    
    private final ProduitRepository repository;
    private final ProduitMapper mapper;  // MapStruct ou custom
    
    public List<ProduitDTO> findAll() {
        return repository.findAll()
            .stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }
    
    public Optional<ProduitDTO> findById(Long id) {
        return repository.findById(id)
            .map(mapper::toDto);
    }
    
    @Transactional
    public ProduitDTO create(ProduitDTO dto) {
        Produit entity = mapper.toEntity(dto);
        Produit saved = repository.save(entity);
        return mapper.toDto(saved);
    }
    
    @Transactional
    public ProduitDTO update(Long id, ProduitDTO dto) {
        Produit entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Produit not found"));
        
        mapper.updateEntity(dto, entity);
        Produit updated = repository.save(entity);
        return mapper.toDto(updated);
    }
    
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
```

**Responsabilités du Service** :
- Implémenter la logique métier
- Convertir Entity → DTO (pour les réponses)
- Convertir DTO → Entity (pour les créations/mises à jour)
- Gérer les transactions (@Transactional)
- Lever des exceptions métier si nécessaire

### 3. Repository Layer (Accès aux données)

**Responsabilité** : Accéder à la base de données via JPA

**Interfaces clés** :
- `JpaRepository<T, ID>` : Fournit les méthodes CRUD automatiquement

**Exemple** :
```java
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    
    // CRUD automatiques héritées de JpaRepository :
    // save(T entity) → INSERT/UPDATE
    // findById(ID id) → SELECT où id = ?
    // findAll() → SELECT *
    // delete(T entity) → DELETE
    // deleteById(ID id) → DELETE où id = ?
    // count() → SELECT COUNT(*)
    // exists(ID id) → SELECT EXISTS(...)
    
    // Méthodes personnalisées :
    List<Produit> findByNomContainingIgnoreCase(String nom);
    
    List<Produit> findByPrixBetween(BigDecimal min, BigDecimal max);
    
    @Query("SELECT p FROM Produit p WHERE p.quantite < :seuil")
    List<Produit> findStockFaible(@Param("seuil") Integer seuil);
}
```

**Responsabilités du Repository** :
- Hériter de JpaRepository
- Fournir les méthodes de recherche simples (nommage Spring Data)
- Implémenter les requêtes complexes avec @Query

### 4. Entity Layer (Modèles de données)

**Responsabilité** : Représenter les tables en base de données

**Annotations clés** :
- `@Entity` : Indique que c'est une entité JPA
- `@Table` : Nom de la table
- `@Id` : Clé primaire
- `@GeneratedValue` : Auto-incrément
- `@Column` : Propriétés de la colonne
- `@Transient` : Champ non persisté

**Exemple** :
```java
@Entity
@Table(name = "produits")
@Data                    // Lombok : génère getters/setters/equals/hashCode
@NoArgsConstructor       // Constructor vide
@AllArgsConstructor      // Constructor avec tous les champs
public class Produit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nom", nullable = false, length = 255)
    private String nom;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "prix", nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;
    
    @Column(name = "quantite", nullable = false)
    private Integer quantite;
    
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
    
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
    
    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
```

### 5. DTO Layer (Data Transfer Objects)

**Responsabilité** : Communiquer les données avec les clients REST

**Annotations clés** :
- `@Data` (Lombok) : génère getters/setters/equals/hashCode
- `@NotNull`, `@NotBlank`, `@Positive`, etc. : Validations

**Exemple** :
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitDTO {
    
    private Long id;
    
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 3, max = 255, message = "Le nom doit faire entre 3 et 255 caractères")
    private String nom;
    
    @NotBlank(message = "La description est obligatoire")
    private String description;
    
    @NotNull(message = "Le prix est obligatoire")
    @Positive(message = "Le prix doit être positif")
    @DecimalMin(value = "0.01", message = "Le prix doit être au moins 0.01")
    private BigDecimal prix;
    
    @NotNull(message = "La quantité est obligatoire")
    @PositiveOrZero(message = "La quantité doit être >= 0")
    private Integer quantite;
}
```

**Avantages des DTOs** :
- Sépare le modèle BD du modèle API
- Permet de valider les données entrantes
- Masque les champs sensibles
- Améliore les performances (projections)

---

## Patterns utilisés

### 1. MVC Pattern (Model-View-Controller)

**Application ici** :
- **Model** : Entity (Produit.java)
- **View** : JSON retourné par l'API
- **Controller** : ProduitController.java

### 2. Repository Pattern

Abstrait l'accès aux données derrière une interface.

```java
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    // Méthodes de requête
}

// Dans le Service :
@RequiredArgsConstructor
public class ProduitService {
    private final ProduitRepository repository;  // Interface
    
    public List<Produit> findAll() {
        return repository.findAll();  // Implémentation cachée
    }
}
```

**Avantages** :
- Facile de changer l'implémentation BD
- Testable (mock du repository)
- Isolé du reste de l'application

### 3. Dependency Injection Pattern

Spring injecte automatiquement les dépendances.

```java
@Service
@RequiredArgsConstructor  // Lombok génère le constructor
public class ProduitService {
    private final ProduitRepository repository;  // Injecté automatiquement
}
```

### 4. DTO Pattern

Sépare les modèles BD et API.

```java
// BD
@Entity
public class Produit { ... }

// API
public class ProduitDTO { ... }

// Conversion dans le Service
mapper.toDto(produit);   // Entity → DTO
mapper.toEntity(dto);    // DTO → Entity
```

### 5. Transactional Pattern

Gère les transactions BD automatiquement.

```java
@Service
public class ProduitService {
    
    @Transactional  // BEGIN TRANSACTION
    public void transfert(Long from, Long to, BigDecimal amount) {
        // Si une erreur survient, tout est rollback automatiquement
        debiterProduit(from, amount);
        crediterProduit(to, amount);
    }  // COMMIT TRANSACTION
}
```

---

## Configuration Spring Boot

### Fichier application.yml

```yaml
spring:
  application:
    name: formation-api
  
  # Configuration de la source de données (BD)
  datasource:
    url: jdbc:postgresql://localhost:5433/formation_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  # Configuration JPA/Hibernate
  jpa:
    hibernate:
      ddl-auto: update  # Options: none, validate, update, create, create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect
  
  # Internationalization
  messages:
    basename: messages
    encoding: UTF-8

# Configuration du serveur
server:
  port: 8085
  servlet:
    context-path: /api

# Configuration du logging
logging:
  level:
    root: INFO
    com.formation: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### Options Hibernate DDL

| Option | Description |
|--------|-------------|
| `validate` | Valide la structure de la BD sans la modifier |
| `update` | Crée les colonnes manquantes (production non recommandé) |
| `create` | Crée les tables à chaque démarrage (destructif) |
| `create-drop` | Crée les tables, les supprime à l'arrêt (tests) |
| `none` | Aucune action (défaut recommandé en production) |

---

## Gestion des erreurs

### Exceptions personnalisées

```java
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### Classe ErrorResponse

```java
@Data
public class ErrorResponse {
    private String code;
    private String message;
    private LocalDateTime timestamp;
    
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
```

---

## Flux d'une requête complète

```
1. CLIENT
   curl -X POST http://localhost:8085/api/produits \
     -H "Content-Type: application/json" \
     -d '{"nom":"Laptop","prix":999.99,"quantite":5}'

2. SPRING REST → ProduitController.createProduit()
   - Spring deserialize le JSON en ProduitDTO
   - @Valid lance la validation (NotBlank, Positive, etc.)
   - Si erreur → GlobalExceptionHandler.handleValidation()

3. CONTROLLER → validation OK
   - Appelle service.create(dto)

4. SERVICE → ProduitService.create()
   - Convertit DTO → Entity (mapper.toEntity)
   - @Transactional : BEGIN TRANSACTION
   - Appelle repository.save(entity)

5. REPOSITORY → JpaRepository.save()
   - Hibernate génère la requête SQL INSERT
   - Envoie à PostgreSQL

6. DATABASE → PostgreSQL
   - INSERT INTO produits (nom, prix, quantite) VALUES (...)
   - Retourne l'entité avec l'ID généré

7. SERVICE → continue
   - Récupère l'Entity avec ID
   - Convertit Entity → DTO (mapper.toDto)
   - @Transactional : COMMIT TRANSACTION
   - Retourne le DTO

8. CONTROLLER → création réussie
   - Crée une ResponseEntity avec status 201 CREATED

9. SPRING REST → Serialization
   - Convertit le ProduitDTO en JSON

10. CLIENT → Reçoit la réponse
    {
      "id": 1,
      "nom": "Laptop",
      "prix": 999.99,
      "quantite": 5
    }
```

---

**Version** : 1.0  
**Dernière mise à jour** : 2026-09-16
