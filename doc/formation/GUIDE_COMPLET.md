# Guide Complet de la Formation Java + Spring Boot

## Table des matières

1. [Présentation générale](#présentation-générale)
2. [Prérequis et installation](#prérequis-et-installation)
3. [Architecture globale](#architecture-globale)
4. [Progression pédagogique](#progression-pédagogique)
5. [Concepts fondamentaux](#concepts-fondamentaux)
6. [Structure du projet](#structure-du-projet)

---

## Présentation générale

Cette formation couvre l'ensemble du stack **Java + Spring Boot + PostgreSQL**, depuis les bases de REST API jusqu'aux patterns avancés. Elle est conçue pour progresser graduellement, avec des étapes clairement définies.

### Objectifs pédagogiques

- ✅ Comprendre l'architecture d'une API REST moderne
- ✅ Maîtriser Spring Boot et ses annotations principales
- ✅ Travailler avec une base de données relationnelle (PostgreSQL)
- ✅ Implémenter les patterns SOLID et MVC
- ✅ Écrire du code robuste et maintenable
- ✅ Tester une application en couches

### Stack technique

| Composant | Version | Rôle |
|-----------|---------|------|
| Java | 17 | Langage principal |
| Spring Boot | 3.5.16 | Framework web |
| Spring Data JPA | 3.5.16 | Accès aux données |
| Spring Validation | 3.5.16 | Validation des payloads |
| PostgreSQL | 16 | Base de données |
| Liquibase | - | Migrations de schéma BD |
| Gradle | 8.x | Build system |
| Lombok | 9.2.0 | Réduction du boilerplate |
| SpringDoc OpenAPI | 2.8.9 | Documentation Swagger |
| JUnit 5 + Mockito | - | Tests unitaires |

---

## Prérequis et installation

### Avant de commencer

```bash
# Vérifier Java
java -version          # Java 17+

# Vérifier Gradle
./gradlew --version    # 8.x
```

### Démarrage rapide

```bash
# 1. Naviguer vers le dossier
cd /home/docker/projects/formation-ia/formation-java

# 2. Lancer l'application (PostgreSQL doit être disponible)
./gradlew bootRun

# 3. Vérifier
curl http://localhost:8085/api/step1/hello

# 4. Swagger UI
http://localhost:8085/api/swagger-ui.html
```

---

## Architecture globale

### Structure des packages

```
src/main/java/com/formation/
│
├── FormationApplication.java           ← Point d'entrée Spring Boot
│
├── step1/                              ← Étape 1 : Hello World
│   └── HelloController.java
│
├── core/                               ← Logique métier par domaine
│   ├── produits/
│   │   ├── controller/ProduitController.java
│   │   ├── dto/
│   │   │   ├── ProduitCreatePayload.java   ← Payload entrée (POST/PUT)
│   │   │   ├── ProduitDto.java             ← DTO sortie
│   │   │   └── ProduitsResponse.java       ← Wrapper liste
│   │   ├── mapper/ProduitMapper.java
│   │   └── service/
│   │       ├── IProduitService.java
│   │       └── ProduitService.java
│   │
│   └── referentiel/                    ← API référentiel (catégories)
│       ├── controller/ReferentielController.java
│       ├── dto/
│       │   ├── CategorieDto.java
│       │   └── ReferentielResponse.java
│       ├── mapper/ReferencielMapper.java
│       └── service/
│           ├── IReferentielService.java
│           └── ReferentielService.java
│
└── engine/                             ← Infrastructure transverse
    ├── config/JpaConfig.java           ← @EnableJpaAuditing
    ├── entities/
    │   ├── ProduitEntity.java
    │   └── CategorieEntity.java
    ├── exceptions/GlobalExceptionHandler.java
    ├── repositories/
    │   ├── ProduitRepository.java
    │   └── CategorieRepository.java
    └── utils/EntityMapper.java
```

### Couches de l'application

```
┌──────────────────────────────────────────────────────┐
│              Controller  (@RestController)            │
│         Reçoit HTTP, délègue au service               │
├──────────────────────────────────────────────────────┤
│              Service  (@Service)                      │
│         Logique métier, orchestration                 │
├──────────────────────────────────────────────────────┤
│              Mapper  (@Component)                     │
│         Conversion Entity ↔ DTO                      │
├──────────────────────────────────────────────────────┤
│              Repository  (@Repository)                │
│         Accès BD via JpaRepository                    │
├──────────────────────────────────────────────────────┤
│              Entity  (@Entity)                        │
│         Modèle de table BD                            │
├──────────────────────────────────────────────────────┤
│              PostgreSQL                               │
└──────────────────────────────────────────────────────┘
```

### Flux d'une requête HTTP

```
1. Client (curl, Postman, Swagger)
         ↓
2. GlobalExceptionHandler — intercepte toutes les erreurs
         ↓
3. Controller — valide (@Valid) et route
         ↓
4. Service — logique métier, lève EntityNotFoundException si besoin
         ↓
5. Mapper — convertit Payload → Entity ou Entity → DTO
         ↓
6. Repository — INSERT / SELECT / UPDATE / DELETE
         ↓
7. Spring Auditing — date_creation / date_modification automatiques
         ↓
Client reçoit le DTO JSON + code HTTP approprié
```

---

## Progression pédagogique

### Phase 1 : Fondamentaux (Étapes 1-2)

**Durée** : 2-3 heures  
**Objectif** : Comprendre REST API et le CRUD complet

#### ETAPE_1 : Hello World API ✅
- Concepts : `@RestController`, `@GetMapping`, `ResponseEntity`
- Fichiers : `HelloController.java`
- Résultat : Premier endpoint REST fonctionnel

#### ETAPE_2 : CRUD Produits ✅
- Concepts : `@Entity`, `@Repository`, `@Service`, payload/DTO séparés, Mapper, Spring Data Auditing
- Fichiers : `ProduitEntity`, `ProduitCreatePayload`, `ProduitDto`, `ProduitRepository`, `ProduitMapper`, `ProduitService`, `ProduitController`
- Particularités :
  - Deux DTOs distincts : `ProduitCreatePayload` (entrée) et `ProduitDto` (sortie)
  - Dates gérées par `@CreatedDate` / `@LastModifiedDate` (pas de `@PrePersist`)
  - `GlobalExceptionHandler` centralisé pour les erreurs
  - Migrations BD via Liquibase
  - Interface de service `IProduitService`
- Résultat : API complète CRUD avec base de données

### Phase 2 : Patterns avancés (Étapes 3-4)

**Durée** : 2-4 heures  
**Objectif** : Requêtes filtrées et gestion d'erreurs avancée

#### ETAPE_3 : Filtres et pagination
- Concepts : `@RequestParam`, pagination Spring Data, filtres dynamiques
- Objectif : Lister les produits avec critères de recherche et pagination

#### ETAPE_4 : Relations
- Concepts : `@OneToMany`, `@ManyToOne`, `FetchType.LAZY`, cascade, orphanRemoval
- Fichiers : `CommandeEntity`, `LigneCommandeEntity`, `StatutCommande`, `CommandeService`, `CommandeController`
- Particularités :
  - Le côté `@ManyToOne` est toujours propriétaire de la relation (porte le `@JoinColumn`)
  - `FetchType.LAZY` partout, `JOIN FETCH` explicite quand nécessaire
  - `prixUnitaire` copié en snapshot sur `LigneCommandeEntity` à la création
  - `CascadeType.ALL` + `orphanRemoval` pour gérer le cycle de vie des lignes
- Résultat : API commandes avec création, consultation et changement de statut
- [Lire ETAPE_4.md](./ETAPE_4.md)

### Phase 3 : Sécurité et Tests (Étapes 5-6)

**Durée** : 3-5 heures  
**Objectif** : Sécuriser et tester l'application

#### ETAPE_5 : Tests avancés
- Concepts : pyramide des tests, `@WebMvcTest`, `@MockitoBean`, `@DataJpaTest`, TestContainers
- Fichiers : `CommandeControllerTest`, `CommandeServiceTest`, `CommandeRepositoryTest`
- Particularités :
  - Tests unitaires sans annotation pour les Mappers et Specifications
  - `@ExtendWith(MockitoExtension.class)` pour les Services (pas de Spring)
  - `@WebMvcTest` pour le routing, la validation et les codes HTTP uniquement
  - `@DataJpaTest` + TestContainers pour tester les requêtes JPA sur PostgreSQL réel
  - `em.flush()` + `em.clear()` pour forcer un vrai aller-retour en base
- Résultat : couverture complète de la couche commandes par niveau de test
- [Lire ETAPE_5.md](./ETAPE_5.md)

#### ETAPE_6 : Authentification et JWT
- Concepts : Spring Security, JWT, Roles/Permissions
- Objectif : Protéger les endpoints

---

## Concepts fondamentaux

### Spring Boot - Annotations principales

#### `@SpringBootApplication`
```java
@SpringBootApplication
public class FormationApplication {
    public static void main(String[] args) {
        SpringApplication.run(FormationApplication.class, args);
    }
}
```
Combine `@Configuration`, `@ComponentScan`, `@EnableAutoConfiguration`.

#### Controller
```java
@RestController               // Retourne JSON directement
@RequestMapping("/api/produit")
@RequiredArgsConstructor      // Lombok : injection par constructeur
public class ProduitController {

    @GetMapping               // GET /api/produit
    @PostMapping              // POST /api/produit
    @PutMapping("/{id}")      // PUT /api/produit/{id}
    @DeleteMapping("/{id}")   // DELETE /api/produit/{id}
}
```

#### Codes HTTP avec `@ResponseStatus`
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)   // 201 — plus simple que ResponseEntity
public ProduitDto createProduit(...) { ... }

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT) // 204
public void deleteProduit(...) { ... }
```

### JPA / Spring Data Auditing

#### Entity avec dates automatiques
```java
@Entity
@Table(name = "produits")
@EntityListeners(AuditingEntityListener.class)  // Active l'auditing
public class ProduitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false) // Jamais modifié après INSERT
    private LocalDateTime date_creation;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime date_modification;     // Mis à jour automatiquement
}
```

```java
// JpaConfig.java — active Spring Data Auditing globalement
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
```

#### Relation ManyToOne
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_categorie")
private CategorieEntity categorie;
```

### Validation Bean Validation

```java
public class ProduitCreatePayload {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal prix;

    @Min(value = 1)
    private Integer uniteParPackaging;

    @NotNull
    private Long idCategorie;
}
```

Déclenché par `@Valid` dans le Controller :
```java
@PostMapping
public ProduitDto createProduit(@Valid @RequestBody ProduitCreatePayload request) { ... }
```

### Gestion globale des erreurs

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return error(404, "Ressource introuvable", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // Retourne les messages de validation consolidés
        return error(400, "Données invalides", ...);
    }
}
```

Format de réponse d'erreur :
```json
{
  "status": 404,
  "error": "Ressource introuvable",
  "message": "Produit introuvable : 99",
  "timestamp": "2026-09-30T10:00:00"
}
```

### Migrations BD avec Liquibase

Les tables sont créées par Liquibase (pas par Hibernate) :
```
src/main/resources/db/changelog/
├── db.changelog-master.yaml          ← Fichier maître
└── changes/
    ├── 001-create-categories.yaml    ← Création table catégories
    ├── 002-insert-categories.yaml    ← Données initiales
    └── 003-create-produits.yaml      ← Création table produits
```

`ddl-auto: none` dans `application.yml` — Hibernate ne touche pas au schéma.

---

## Structure du projet

### Répertoire des fichiers

```
formation-java/
├── src/
│   ├── main/
│   │   ├── java/com/formation/      ← Code source (voir ci-dessus)
│   │   └── resources/
│   │       ├── application.yml      ← Configuration Spring Boot
│   │       └── db/changelog/        ← Migrations Liquibase
│   └── test/
│       └── java/com/formation/      ← Tests unitaires (Mockito)
│
├── doc/formation/
│   ├── INDEX.md                     ← Navigation
│   ├── GUIDE_COMPLET.md             ← Ce guide
│   ├── ARCHITECTURE.md              ← Détails architecture
│   ├── CONCEPTS.md                  ← Concepts en détail
│   ├── SPRING_BOOT.md               ← Référence Spring Boot
│   ├── RESSOURCES.md                ← Ressources externes
│   ├── ETAPE_1.md                   ← Étape 1
│   └── ETAPE_2.md                   ← Étape 2
│
├── build.gradle.kts                 ← Dépendances Gradle
├── settings.gradle
└── gradlew                          ← Wrapper Gradle
```

### Configuration de l'application

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/formation_db
    username: postgres
    password: postgres

  jpa:
    hibernate:
      ddl-auto: none      # Liquibase gère le schéma
    show-sql: false

server:
  port: 8085
  servlet:
    context-path: /api    # Tous les endpoints sont préfixés /api

springdoc:
  swagger-ui:
    path: /swagger-ui.html  # http://localhost:8085/api/swagger-ui.html
```

---

## Commandes utiles

### Gradle

```bash
./gradlew bootRun           # Lancer l'app
./gradlew build             # Build JAR
./gradlew test              # Lancer tous les tests
./gradlew clean             # Nettoyer le build
```

### cURL - Tester les endpoints

```bash
# Hello World (Étape 1)
curl http://localhost:8085/api/step1/hello

# Lister les produits
curl http://localhost:8085/api/produit

# Créer un produit
curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -d '{"nom":"Café arabica","prix":12.50,"uniteParPackaging":6,"idCategorie":1}'

# Mettre à jour
curl -X PUT http://localhost:8085/api/produit/1 \
  -H "Content-Type: application/json" \
  -d '{"nom":"Café robusta","prix":9.99,"uniteParPackaging":12,"idCategorie":1}'

# Supprimer
curl -X DELETE http://localhost:8085/api/produit/1

# Swagger UI
open http://localhost:8085/api/swagger-ui.html
```

---

## Prochaines étapes

1. Lire [ETAPE_1.md](./ETAPE_1.md) — Hello World REST
2. Lire [ETAPE_2.md](./ETAPE_2.md) — CRUD Produits complet
3. Consulter [ARCHITECTURE.md](./ARCHITECTURE.md) pour les détails techniques
4. Consulter [CONCEPTS.md](./CONCEPTS.md) pour approfondir les concepts

---

*Dernière mise à jour : 2026-09-30*
