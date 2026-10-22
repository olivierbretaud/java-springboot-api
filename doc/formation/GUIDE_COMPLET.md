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
- ✅ Sécuriser une API avec Spring Security et JWT
- ✅ Tester une application en couches

### Stack technique

| Composant | Version | Rôle |
|-----------|---------|------|
| Java | 17 | Langage principal |
| Spring Boot | 3.5.16 | Framework web |
| Spring Data JPA | 3.5.16 | Accès aux données |
| Spring Security | 3.5.16 | Authentification et autorisation |
| Spring Validation | 3.5.16 | Validation des payloads |
| PostgreSQL | 16 | Base de données |
| Liquibase | - | Migrations de schéma BD |
| Gradle | 8.x | Build system |
| Lombok | 9.2.0 | Réduction du boilerplate |
| SpringDoc OpenAPI | 2.8.9 | Documentation Swagger |
| JJWT | 0.12.6 | Génération et validation de tokens JWT |
| JUnit 5 + Mockito | - | Tests unitaires |
| TestContainers | - | Tests d'intégration avec PostgreSQL réel |

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
├── security/                           ← Infrastructure Spring Security
│   ├── SecurityConfig.java             ← Règles d'accès, CSRF, session stateless
│   ├── JwtService.java                 ← Génération et validation des tokens JWT
│   ├── JwtAuthenticationFilter.java    ← Filtre HTTP : lit le Bearer token
│   ├── JwtAuthenticationEntryPoint.java← Retourne 401 si non authentifié
│   ├── CustomAccessDeniedHandler.java  ← Retourne 403 si rôle insuffisant
│   └── UtilisateurDetailsService.java  ← UserDetailsService → charge depuis la BD
│
├── core/                               ← Logique métier par domaine
│   ├── auth/                           ← Authentification (login, reset password)
│   │   ├── controller/AuthController.java
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   ├── AuthResponse.java       ← { token }
│   │   │   ├── ProfileResponse.java
│   │   │   ├── ResetPasswordRequest.java
│   │   │   ├── ResetPasswordResponse.java
│   │   │   ├── ChangePasswordRequest.java
│   │   │   └── ChangePasswordResponse.java
│   │   └── service/
│   │       ├── AuthService.java        ← login(), resetPassword(), changePassword()
│   │       └── MailService.java        ← envoi du lien de reset par email
│   │
│   ├── produits/
│   │   ├── controller/ProduitController.java
│   │   ├── dto/
│   │   │   ├── ProduitCreatePayload.java   ← Payload entrée (POST/PUT)
│   │   │   ├── ProduitDto.java             ← DTO sortie
│   │   │   ├── ProduitsResponse.java       ← Wrapper liste simple
│   │   │   └── ProduitSearchParams.java    ← extends PageParams + filtres
│   │   ├── mapper/ProduitMapper.java
│   │   ├── service/
│   │   │   ├── IProduitService.java
│   │   │   └── ProduitService.java
│   │   └── specification/
│   │       └── ProduitSpecification.java   ← Filtres dynamiques JPA Criteria
│   │
│   ├── commandes/                      ← API commandes (étape 4)
│   │   ├── controller/CommandeController.java
│   │   ├── dto/
│   │   │   ├── CommandeCreatePayload.java
│   │   │   ├── LigneCommandePayload.java
│   │   │   ├── CommandeDto.java
│   │   │   ├── LigneCommandeDto.java
│   │   │   ├── CommandeListResponse.java
│   │   │   └── ChangerStatutPayload.java
│   │   ├── mapper/CommandeMapper.java
│   │   └── service/
│   │       ├── ICommandeService.java
│   │       └── CommandeService.java
│   │
│   └── referentiel/                    ← API référentiel (catégories, rôles, statuts)
│       ├── controller/ReferentielController.java
│       ├── dto/
│       │   ├── CategorieDto.java
│       │   ├── RoleDto.java
│       │   ├── StatutCommandeDto.java
│       │   └── ReferentielResponse.java
│       ├── mapper/ReferencielMapper.java
│       └── service/
│           ├── IReferentielService.java
│           └── ReferentielService.java
│
└── engine/                             ← Infrastructure transverse
    ├── config/
    │   ├── JpaConfig.java              ← @EnableJpaAuditing
    │   └── OpenApiConfig.java          ← Swagger avec support Bearer token
    ├── entities/
    │   ├── ProduitEntity.java
    │   ├── CategorieEntity.java
    │   ├── CommandeEntity.java
    │   ├── LigneCommandeEntity.java
    │   ├── StatutCommandeEntity.java
    │   ├── UtilisateurEntity.java      ← implements UserDetails
    │   └── RoleEntity.java
    ├── exceptions/
    │   ├── GlobalExceptionHandler.java
    │   ├── CommandeNonSupprimableException.java
    │   └── ResetTokenInvalidException.java
    ├── pagination/                     ← Infrastructure réutilisable
    │   ├── PageParams.java             ← Classe abstraite de base (page, size, sort)
    │   └── PageResponse.java           ← Réponse paginée générique <T>
    ├── repositories/
    │   ├── ProduitRepository.java      ← + JpaSpecificationExecutor
    │   ├── CategorieRepository.java
    │   ├── CommandeRepository.java
    │   ├── StatutCommandeRepository.java
    │   ├── UtilisateurRepository.java
    │   └── RoleRepository.java
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
2. JwtAuthenticationFilter — vérifie le Bearer token, charge l'utilisateur
         ↓
3. GlobalExceptionHandler — intercepte toutes les erreurs
         ↓
4. Controller — valide (@Valid) et route
         ↓
5. Service — logique métier, lève EntityNotFoundException si besoin
         ↓
6. Mapper — convertit Payload → Entity ou Entity → DTO
         ↓
7. Repository — INSERT / SELECT / UPDATE / DELETE
         ↓
8. Spring Auditing — date_creation / date_modification automatiques
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
- [Lire ETAPE_1.md](./ETAPE_1.md)

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
- [Lire ETAPE_2.md](./ETAPE_2.md)

### Phase 2 : Patterns avancés (Étapes 3-4)

**Durée** : 2-4 heures  
**Objectif** : Requêtes filtrées, pagination et relations entre entités

#### ETAPE_3 : Filtres et pagination ✅
- Concepts : `@RequestParam`, `Specification<T>`, `JpaSpecificationExecutor`, pagination Spring Data
- Fichiers : `ProduitSpecification`, `ProduitSearchParams`, `PageParams`, `PageResponse`
- Particularités :
  - `PageParams` (abstraite) est réutilisable pour tout futur endpoint paginé
  - `ProduitSearchParams extends PageParams` ajoute les filtres spécifiques aux produits
  - `ProduitSpecification.from(params)` construit les prédicats SQL dynamiquement
  - `ProduitRepository` implémente `JpaSpecificationExecutor` pour accéder à `findAll(Spec, Pageable)`
- Résultat : `GET /api/produit/search?nom=café&prixMax=15&page=0&size=10`
- [Lire ETAPE_3.md](./ETAPE_3.md)

#### ETAPE_4 : Relations ✅
- Concepts : `@OneToMany`, `@ManyToOne`, `FetchType.LAZY`, cascade, orphanRemoval
- Fichiers : `CommandeEntity`, `LigneCommandeEntity`, `StatutCommandeEntity`, `CommandeService`, `CommandeController`
- Particularités :
  - Le côté `@ManyToOne` est toujours propriétaire de la relation (porte le `@JoinColumn`)
  - `FetchType.LAZY` partout, `JOIN FETCH` explicite quand nécessaire
  - `prixUnitaire` copié en snapshot sur `LigneCommandeEntity` à la création
  - `CascadeType.ALL` + `orphanRemoval` pour gérer le cycle de vie des lignes
  - `StatutCommandeEntity` en table de référence (pas un `enum` Java)
- Résultat : API commandes avec création, consultation et changement de statut
- [Lire ETAPE_4.md](./ETAPE_4.md)

### Phase 3 : Sécurité et Tests (Étapes 5-6)

**Durée** : 3-5 heures  
**Objectif** : Sécuriser et tester l'application

#### ETAPE_5 : Authentification et JWT ✅
- Concepts : Spring Security, JWT, `UserDetailsService`, BCrypt, filtres HTTP, rôles
- Fichiers : `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `AuthController`, `AuthService`, `UtilisateurEntity`, `RoleEntity`
- Particularités :
  - Session **stateless** — pas de session serveur, chaque requête porte son token
  - `JwtAuthenticationFilter extends OncePerRequestFilter` intercepte le `Bearer` header
  - `UtilisateurEntity implements UserDetails` — Spring Security charge l'utilisateur directement depuis la BD
  - `BCryptPasswordEncoder` pour le hachage des mots de passe (jamais en clair)
  - `@EnableMethodSecurity` pour protéger par rôle avec `@PreAuthorize`
  - Flux reset password : lien par email avec token à usage unique
  - Endpoints publics : `/auth/login`, `/auth/reset-password`, `/auth/change-password`, Swagger
- Résultat : tous les endpoints protégés, login via JWT Bearer token
- [Lire ETAPE_5.md](./ETAPE_5.md)

#### ETAPE_6 : Tests avancés ✅
- Concepts : pyramide des tests, `@WebMvcTest`, `@MockitoBean`, `@DataJpaTest`, TestContainers
- Fichiers : `CommandeControllerTest`, `CommandeServiceTest`, `CommandeRepositoryTest`
- Particularités :
  - Tests unitaires sans annotation Spring pour les Mappers et Specifications
  - `@ExtendWith(MockitoExtension.class)` pour les Services (pas de Spring)
  - `@WebMvcTest` pour le routing, la validation et les codes HTTP uniquement
  - `@DataJpaTest` + TestContainers pour tester les requêtes JPA sur PostgreSQL réel
  - `em.flush()` + `em.clear()` pour forcer un vrai aller-retour en base
  - `@WithMockUser` pour simuler un utilisateur authentifié dans les tests de Controller sécurisés
- Résultat : couverture complète de la couche commandes par niveau de test
- [Lire ETAPE_6.md](./ETAPE_6.md)

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
├── db.changelog-master.yaml               ← Fichier maître
└── changes/
    ├── 001-create-categories.yaml         ← Table catégories
    ├── 002-insert-categories.yaml         ← Données initiales catégories
    ├── 003-create-produits.yaml           ← Table produits
    ├── 004-create-utilisateurs.yaml       ← Table utilisateurs (avec password, role)
    ├── 005-add-prenom-utilisateurs.yaml   ← Ajout colonne prenom
    ├── 006-insert-premier-utilisateur.yaml← Utilisateur admin initial
    ├── 007-add-id-utilisateur-roles.yaml  ← Colonne FK vers roles
    ├── 008-create-commandes.yaml          ← Table commandes
    ├── 009-create-lignes-commande.yaml    ← Table lignes_commande
    ├── 010-create-statuts-commande.yaml   ← Table statuts_commande (référentiel)
    ├── 011-insert-statuts-commande.yaml   ← Données initiales statuts
    ├── 012-migrate-commandes-statut.yaml  ← Migration FK statut sur commandes
    ├── 013-add-id-to-statuts-commande.yaml
    ├── 014-create-roles.yaml              ← Table roles (référentiel)
    ├── 015-migrate-utilisateur-role.yaml  ← Migration FK role sur utilisateurs
    └── 016-add-reset-token-utilisateurs.yaml ← Token reset password
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
│   │       └── db/changelog/        ← Migrations Liquibase (016 fichiers)
│   └── test/
│       └── java/com/formation/      ← Tests (unitaires, @WebMvcTest, @DataJpaTest)
│
├── doc/formation/
│   ├── INDEX.md                     ← Navigation
│   ├── GUIDE_COMPLET.md             ← Ce guide
│   ├── DATABASE.md                  ← Schéma BD (tables, FK, migrations)
│   ├── ARCHITECTURE.md              ← Détails architecture
│   ├── CONCEPTS.md                  ← Concepts en détail
│   ├── SPRING_BOOT.md               ← Référence Spring Boot
│   ├── RESSOURCES.md                ← Ressources externes
│   ├── ETAPE_1.md                   ← Étape 1 : Hello World
│   ├── ETAPE_2.md                   ← Étape 2 : CRUD Produits
│   ├── ETAPE_3.md                   ← Étape 3 : Pagination et Filtres
│   ├── ETAPE_4.md                   ← Étape 4 : Relations (Commandes)
│   ├── ETAPE_5.md                   ← Étape 5 : Authentification JWT
│   └── ETAPE_6.md                   ← Étape 6 : Tests avancés
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

# --- Authentification (Étape 5) ---

# Login — récupérer un token JWT
curl -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@formation.com","password":"admin123"}'
# Réponse : { "token": "eyJ..." }

# Stocker le token dans une variable
TOKEN=$(curl -s -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@formation.com","password":"admin123"}' | jq -r '.token')

# Profil de l'utilisateur connecté
curl http://localhost:8085/api/auth/profile \
  -H "Authorization: Bearer $TOKEN"

# --- Produits (nécessite un token) ---

# Lister les produits
curl http://localhost:8085/api/produit \
  -H "Authorization: Bearer $TOKEN"

# Recherche filtrée avec pagination (Étape 3)
curl "http://localhost:8085/api/produit/search?nom=café&prixMax=15&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Créer un produit
curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"nom":"Café arabica","prix":12.50,"uniteParPackaging":6,"idCategorie":1}'

# Mettre à jour
curl -X PUT http://localhost:8085/api/produit/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"nom":"Café robusta","prix":9.99,"uniteParPackaging":12,"idCategorie":1}'

# Supprimer
curl -X DELETE http://localhost:8085/api/produit/1 \
  -H "Authorization: Bearer $TOKEN"

# --- Commandes (Étape 4) ---

# Créer une commande
curl -X POST "http://localhost:8085/api/commandes?utilisateurId=1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"lignes":[{"idProduit":1,"quantite":2}]}'

# Swagger UI (public, pas de token requis)
open http://localhost:8085/api/swagger-ui.html
```

---

## Parcours de lecture recommandé

1. Lire [ETAPE_1.md](./ETAPE_1.md) — Hello World REST
2. Lire [ETAPE_2.md](./ETAPE_2.md) — CRUD Produits complet
3. Lire [ETAPE_3.md](./ETAPE_3.md) — Pagination et filtres dynamiques
4. Lire [ETAPE_4.md](./ETAPE_4.md) — Relations JPA et API commandes
5. Lire [ETAPE_5.md](./ETAPE_5.md) — Authentification Spring Security + JWT
6. Lire [ETAPE_6.md](./ETAPE_6.md) — Pyramide des tests (Mockito, @WebMvcTest, @DataJpaTest)
7. Consulter [ARCHITECTURE.md](./ARCHITECTURE.md) pour les détails techniques
8. Consulter [CONCEPTS.md](./CONCEPTS.md) pour approfondir les concepts

---

*Dernière mise à jour : 2026-10-22*
