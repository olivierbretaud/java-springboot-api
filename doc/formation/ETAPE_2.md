# ÉTAPE 2 : CRUD Produits

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Prérequis](#prérequis)
3. [Concepts clés](#concepts-clés)
4. [Architecture CRUD](#architecture-crud)
5. [Fichiers implémentés](#fichiers-implémentés)
6. [Tests](#tests)
7. [Exercices](#exercices)
8. [Prochaines étapes](#prochaines-étapes)

---

## 🎯 Objectif

**Créer une API complète pour gérer des produits en base de données PostgreSQL.**

Après cette étape, vous allez pouvoir :
- ✅ Créer une **Entity** JPA (table BD)
- ✅ Créer des **DTOs** (modèles API : réponse et payload)
- ✅ Créer un **Repository** (accès BD)
- ✅ Créer un **Mapper** (conversion Entity ↔ DTO)
- ✅ Créer un **Service** (logique métier)
- ✅ Créer un **Controller** (endpoints REST)
- ✅ Implémenter les 4 verbes HTTP : GET, POST, PUT, DELETE

---

## 📋 Prérequis

- ✅ Avoir compris ETAPE_1
- ✅ Connaître les concepts : Entity, DTO, Repository, Service

---

## 📖 Concepts clés

### 1. Entity - Modèle en base de données

Une **Entity** représente une table en base de données.

```java
@Entity
@Table(name = "produits")
public class ProduitEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String nom;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal prix;  // Decimal(10, 2)
}
```

**Annotations JPA principales** :
| Annotation | Utilité |
|-----------|---------|
| `@Entity` | C'est une entité JPA → table BD |
| `@Table(name = "...")` | Nom de la table |
| `@Id` | Clé primaire |
| `@GeneratedValue` | Auto-incrément |
| `@Column` | Configuration de la colonne |

### 2. Gestion des dates avec Spring Data Auditing

Plutôt que d'utiliser `@PrePersist`/`@PreUpdate` manuellement, on délègue à **Spring Data Auditing** qui gère automatiquement les dates de création et modification.

**Configuration (déjà en place)** :

```java
// JpaConfig.java
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
```

**Dans l'Entity** :

```java
@EntityListeners(AuditingEntityListener.class)
public class ProduitEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime date_creation;   // rempli au INSERT, jamais modifié

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime date_modification; // mis à jour à chaque UPDATE
}
```

**Pourquoi c'est mieux que `@PrePersist`/`@PreUpdate`** :
| | `@PrePersist` / `@PreUpdate` | Spring Data Auditing |
|---|---|---|
| Code | Méthode à écrire dans chaque entité | Deux annotations sur les champs |
| Centralisation | Non — à répéter partout | Oui — une config globale |
| Testabilité | Difficile (`LocalDateTime.now()` direct) | Mockable via `DateTimeProvider` |
| Standard Spring | Non | Oui |

### 3. DTO - Modèles de l'API

On sépare le **payload** (données en entrée) du **DTO** (données en sortie).

**ProduitCreatePayload** — payload d'entrée pour POST et PUT :
```java
@Getter
@Setter
public class ProduitCreatePayload {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal prix;

    @Min(value = 1)
    private Integer uniteParPackaging;

    @NotNull(message = "La catégorie est obligatoire")
    private Long idCategorie;
}
```

**ProduitDto** — DTO de sortie (réponse API) :
```java
@Getter
@Setter
public class ProduitDto {
    private Long id;
    private String nom;
    private String description;
    private BigDecimal prix;
    private Integer uniteParPackaging;
    private Long categorieId;
    private String categorieName;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
```

**ProduitsResponse** — wrapper pour la liste :
```java
@Getter
@RequiredArgsConstructor
public class ProduitsResponse {
    private final List<ProduitDto> produits;
}
```

### 4. Repository - Accès à la base de données

```java
@Repository
public interface ProduitRepository extends JpaRepository<ProduitEntity, Long> {
    // Hérite automatiquement de :
    // - save(ProduitEntity) → INSERT/UPDATE
    // - findById(Long) → SELECT WHERE id = ?
    // - findAll() → SELECT *
    // - existsById(Long) → SELECT count(*) WHERE id = ?
    // - deleteById(Long) → DELETE WHERE id = ?
}
```

### 5. Mapper - Conversion Entity ↔ DTO

Le **Mapper** centralise la conversion entre Entity et DTO, évitant ce code dans le Service.

```java
@Component
@RequiredArgsConstructor
public class ProduitMapper {

    private final EntityMapper entityMapper;

    public ProduitDto toProduitDto(ProduitEntity entity) {
        return entityMapper.toDto(entity, e -> {
            ProduitDto dto = new ProduitDto();
            dto.setId(e.getId());
            dto.setNom(e.getNom());
            // ...
            if (e.getCategorie() != null) {
                dto.setCategorieId(e.getCategorie().getId());
                dto.setCategorieName(e.getCategorie().getNom());
            }
            dto.setDateCreation(e.getDate_creation());
            dto.setDateModification(e.getDate_modification());
            return dto;
        });
    }

    public List<ProduitDto> toProduitDtoList(List<ProduitEntity> entities) {
        return entityMapper.toDtoList(entities, this::toProduitDto);
    }

    public ProduitEntity toEntity(ProduitCreatePayload request, CategorieEntity categorie) {
        ProduitEntity entity = new ProduitEntity();
        entity.setNom(request.getNom());
        entity.setDescription(request.getDescription());
        entity.setPrix(request.getPrix());
        entity.setUniteParPackaging(request.getUniteParPackaging());
        entity.setCategorie(categorie);
        return entity;
    }
}
```

### 6. Service - Logique métier

```java
@Service
@RequiredArgsConstructor
public class ProduitService implements IProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final ProduitMapper produitMapper;

    @Override
    public List<ProduitDto> listProduits() {
        return produitMapper.toProduitDtoList(produitRepository.findAll());
    }

    @Override
    public ProduitDto getProduitById(Long id) {
        return produitMapper.toProduitDto(
            produitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable : " + id))
        );
    }

    @Override
    public ProduitDto createProduit(ProduitCreatePayload request) {
        CategorieEntity categorie = categorieRepository.findById(request.getIdCategorie())
            .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable : " + request.getIdCategorie()));

        return produitMapper.toProduitDto(
            produitRepository.save(produitMapper.toEntity(request, categorie))
        );
    }

    @Override
    public ProduitDto updateProduit(Long id, ProduitCreatePayload request) {
        ProduitEntity produit = produitRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Produit introuvable : " + id));

        CategorieEntity categorie = categorieRepository.findById(request.getIdCategorie())
            .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable : " + request.getIdCategorie()));

        produit.setNom(request.getNom());
        produit.setDescription(request.getDescription());
        produit.setPrix(request.getPrix());
        produit.setUniteParPackaging(request.getUniteParPackaging());
        produit.setCategorie(categorie);

        return produitMapper.toProduitDto(produitRepository.save(produit));
    }

    @Override
    public void deleteProduit(Long id) {
        if (!produitRepository.existsById(id)) {
            throw new EntityNotFoundException("Produit introuvable : " + id);
        }
        produitRepository.deleteById(id);
    }
}
```

### 7. Controller - Endpoints REST

```java
@RestController
@RequestMapping("/api/produit")
@RequiredArgsConstructor
public class ProduitController {

    private final IProduitService produitService;

    @GetMapping
    public ProduitsResponse getProduits() {
        return new ProduitsResponse(produitService.listProduits());
    }

    @GetMapping("/{id}")
    public ProduitDto getProduitById(@PathVariable Long id) {
        return produitService.getProduitById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProduitDto createProduit(@Valid @RequestBody ProduitCreatePayload request) {
        return produitService.createProduit(request);
    }

    @PutMapping("/{id}")
    public ProduitDto updateProduit(@PathVariable Long id, @Valid @RequestBody ProduitCreatePayload request) {
        return produitService.updateProduit(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduit(@PathVariable Long id) {
        produitService.deleteProduit(id);
    }
}
```

---

## 🏗️ Architecture CRUD

### Flux complet d'une requête POST

```
CLIENT (curl)
    ↓
@PostMapping → ProduitController.createProduit()
    ↓
@Valid → Validation du ProduitCreatePayload
    ↓
ProduitService.createProduit()
    ↓
categorieRepository.findById() → CategorieEntity
    ↓
ProduitMapper.toEntity(request, categorie) → ProduitEntity
    ↓
ProduitRepository.save() → INSERT INTO produits ...
    ↓
Spring Auditing → date_creation & date_modification automatiques
    ↓
ProduitMapper.toProduitDto(entity) → ProduitDto
    ↓
HTTP 201 CREATED + JSON
    ↓
CLIENT reçoit le ProduitDto avec ID et dates
```

### Les 5 opérations

| Opération | Verbe HTTP | Endpoint | Réponse |
|-----------|-----------|----------|---------|
| Lister | GET | `/api/produit` | `ProduitsResponse` (200) |
| Lire | GET | `/api/produit/{id}` | `ProduitDto` (200) |
| Créer | POST | `/api/produit` | `ProduitDto` (201) |
| Mettre à jour | PUT | `/api/produit/{id}` | `ProduitDto` (200) |
| Supprimer | DELETE | `/api/produit/{id}` | vide (204) |

---

## 🧪 Tests

### Préalable : Lancer l'application

```bash
./gradlew bootRun
```

### Test 1 : GET tous les produits

```bash
curl http://localhost:8085/api/produit
```

**Résultat attendu** :
```json
{ "produits": [] }
```

### Test 2 : CREATE - Créer un produit

```bash
curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Café arabica",
    "description": "Café d'\''origine éthiopienne",
    "prix": 12.50,
    "uniteParPackaging": 6,
    "idCategorie": 1
  }'
```

**Résultat attendu** : Status 201 CREATED
```json
{
  "id": 1,
  "nom": "Café arabica",
  "description": "Café d'origine éthiopienne",
  "prix": 12.50,
  "uniteParPackaging": 6,
  "categorieId": 1,
  "categorieName": "Boissons",
  "dateCreation": "2026-09-30T10:00:00",
  "dateModification": "2026-09-30T10:00:00"
}
```

### Test 3 : GET par ID

```bash
curl http://localhost:8085/api/produit/1
```

### Test 4 : UPDATE - Mettre à jour un produit

```bash
curl -X PUT http://localhost:8085/api/produit/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Café robusta",
    "description": "Café corsé",
    "prix": 9.99,
    "uniteParPackaging": 12,
    "idCategorie": 1
  }'
```

**Résultat attendu** : Status 200 OK + `dateModification` mise à jour automatiquement

### Test 5 : DELETE - Supprimer un produit

```bash
curl -X DELETE http://localhost:8085/api/produit/1
```

**Résultat attendu** : Status 204 No Content

### Test 6 : Validation - Données invalides

```bash
curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "",
    "prix": -5,
    "idCategorie": null
  }'
```

**Résultat attendu** : Status 400 Bad Request + erreurs de validation

---

## 💡 Exercices

### Exercice 1 : Créer plusieurs produits

Créez 3 produits dans la même catégorie.

```bash
curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -d '{"nom":"Pomme","description":"Pomme golden","prix":2.50,"uniteParPackaging":6,"idCategorie":1}'

curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -d '{"nom":"Poire","description":"Poire williams","prix":3.20,"uniteParPackaging":4,"idCategorie":1}'

curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -d '{"nom":"Banane","description":"Banane bio","prix":1.80,"uniteParPackaging":8,"idCategorie":1}'
```

### Exercice 2 : Vérifier la gestion des erreurs

Testez les cas d'erreur :

```bash
# Produit inexistant
curl http://localhost:8085/api/produit/999

# Catégorie inexistante
curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -d '{"nom":"Test","prix":1.00,"idCategorie":999}'
```

**Résultat attendu** : Status 404 + message d'erreur

---

## ✅ Prochaines étapes

Quand vous êtes à l'aise avec cette étape :

1. ✅ Vous comprenez Entity, DTO, Repository, Mapper, Service, Controller
2. ✅ Vous comprenez Spring Data Auditing pour les dates
3. ✅ Vous savez séparer payload d'entrée et DTO de sortie
4. ✅ Vous pouvez implémenter un CRUD complet en architecture en couches

**Prochaines améliorations** :
- Ajouter la pagination des résultats
- Ajouter des relations OneToMany/ManyToMany supplémentaires
- Ajouter des filtres de recherche par critères

---

**Version** : 2.0  
**Dernière mise à jour** : 2026-09-30
