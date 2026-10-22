# ÉTAPE 3 : Pagination et Filtres

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Concepts clés](#concepts-clés)
3. [Architecture](#architecture)
4. [Fichiers implémentés](#fichiers-implémentés)
5. [Tests](#tests)
6. [Prochaines étapes](#prochaines-étapes)

---

## 🎯 Objectif

**Ajouter un endpoint de recherche paginé et filtrable sur les produits, avec une infrastructure réutilisable pour tous les futurs endpoints.**

Après cette étape, vous allez pouvoir :
- ✅ Filtrer par `nom`, `categorieId`, `prixMin`, `prixMax` via des query params
- ✅ Paginer les résultats avec `page`, `size`, `sort`, `direction`
- ✅ Réutiliser l'infrastructure de pagination sur n'importe quel endpoint
- ✅ Construire des filtres dynamiques avec `Specification<T>`

---

## 📖 Concepts clés

### 1. Query params avec `@RequestParam` et valeurs par défaut

```java
@GetMapping("/search")
public PageResponse<ProduitDto> searchProduits(
        @RequestParam(required = false) String nom,        // optionnel
        @RequestParam(defaultValue = "0") int page,        // défaut : 0
        @RequestParam(defaultValue = "10") int size,       // défaut : 10
        @RequestParam(defaultValue = "nom") String sort,   // défaut : tri par nom
        @RequestParam(defaultValue = "ASC") String direction) {
    // ...
}
```

**Exemples d'appels** :
```bash
# Valeurs par défaut (page 0, 10 résultats, triés par nom ASC)
GET /api/produit/search

# Avec filtres
GET /api/produit/search?nom=café&prixMax=15.00

# Avec pagination personnalisée
GET /api/produit/search?page=1&size=5&sort=prix&direction=DESC

# Tout combiné
GET /api/produit/search?nom=café&categorieId=2&prixMin=5&prixMax=20&page=0&size=10
```

### 2. Infrastructure de pagination réutilisable — `engine/pagination/`

#### `PageParams` — classe de base pour les paramètres de pagination

```java
public abstract class PageParams {

    private int page = 0;
    private int size = 10;
    private String sort = "id";
    private String direction = "ASC";

    public Pageable toPageable() {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
            .orElse(Sort.Direction.ASC);
        return PageRequest.of(page, size, Sort.by(dir, sort));
    }
}
```

Pour **réutiliser sur un autre endpoint**, il suffit d'étendre `PageParams` :
```java
// Pour un futur endpoint de catégories
public class CategorieSearchParams extends PageParams {
    private String nom;
    // ... filtres spécifiques
}
```

#### `PageResponse<T>` — réponse paginée générique

```java
@Getter
public class PageResponse<T> {
    private final List<T> contenu;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean dernierePage;

    public PageResponse(Page<T> page) { ... }
}
```

Réponse JSON retournée :
```json
{
  "contenu": [...],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "dernierePage": false
}
```

### 3. Filtres dynamiques avec `Specification<T>`

`Specification` permet de construire des requêtes SQL dynamiques selon les paramètres fournis. Un filtre non renseigné est simplement ignoré.

```java
public class ProduitSpecification {

    public static Specification<ProduitEntity> from(ProduitSearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtre nom — LIKE insensible à la casse
            if (params.getNom() != null && !params.getNom().isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("nom")),
                    "%" + params.getNom().toLowerCase() + "%"
                ));
            }

            // Filtre catégorie — jointure sur l'id
            if (params.getCategorieId() != null) {
                predicates.add(cb.equal(
                    root.get("categorie").get("id"),
                    params.getCategorieId()
                ));
            }

            // Filtre prix min / max
            if (params.getPrixMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("prix"), params.getPrixMin()));
            }
            if (params.getPrixMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("prix"), params.getPrixMax()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

**SQL généré** (exemple avec `nom=café` et `prixMax=15`) :
```sql
SELECT * FROM produits
WHERE LOWER(nom) LIKE '%café%'
  AND prix <= 15
ORDER BY nom ASC
LIMIT 10 OFFSET 0
```

### 4. `JpaSpecificationExecutor` dans le Repository

Pour utiliser les Specifications, le repository doit implémenter `JpaSpecificationExecutor` :

```java
@Repository
public interface ProduitRepository
        extends JpaRepository<ProduitEntity, Long>,
                JpaSpecificationExecutor<ProduitEntity> {
    // Donne accès à findAll(Specification, Pageable)
}
```

---

## 🏗️ Architecture

### Nouveaux fichiers

```
src/main/java/com/formation/
│
├── engine/pagination/                     ← Infrastructure réutilisable
│   ├── PageParams.java                    ← Classe de base pagination
│   └── PageResponse.java                  ← Réponse paginée générique <T>
│
└── core/produits/
    ├── dto/
    │   ├── ProduitSearchParams.java        ← extends PageParams + filtres produit
    │   └── (ProduitsPageResponse supprimé → remplacé par PageResponse<ProduitDto>)
    └── specification/
        └── ProduitSpecification.java       ← Filtres dynamiques JPA
```

### Flux de la requête de recherche

```
GET /api/produit/search?nom=café&page=0&size=10
         ↓
ProduitController.searchProduits()
  → construit ProduitSearchParams à partir des @RequestParam
         ↓
ProduitService.searchProduits(params)
  → ProduitSpecification.from(params) → Specification<ProduitEntity>
  → params.toPageable() → Pageable (page=0, size=10, sort=nom ASC)
         ↓
ProduitRepository.findAll(specification, pageable)
  → SQL filtré + paginé
         ↓
Page<ProduitEntity>.map(produitMapper::toProduitDto)
  → Page<ProduitDto>
         ↓
new PageResponse<>(page) → JSON { contenu, page, size, totalElements, ... }
```

---

## 💻 Fichiers implémentés

### `PageParams.java`

**Chemin** : `src/main/java/com/formation/engine/pagination/PageParams.java`

```java
public abstract class PageParams {
    private int page = 0;
    private int size = 10;
    private String sort = "id";
    private String direction = "ASC";

    public Pageable toPageable() {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
            .orElse(Sort.Direction.ASC);
        return PageRequest.of(page, size, Sort.by(dir, sort));
    }
}
```

### `PageResponse.java`

**Chemin** : `src/main/java/com/formation/engine/pagination/PageResponse.java`

```java
@Getter
public class PageResponse<T> {
    private final List<T> contenu;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean dernierePage;

    public PageResponse(Page<T> page) { /* ... */ }
}
```

### `ProduitSearchParams.java`

**Chemin** : `src/main/java/com/formation/core/produits/dto/ProduitSearchParams.java`

```java
@Getter
@Setter
public class ProduitSearchParams extends PageParams {
    private String nom;
    private Long categorieId;
    private BigDecimal prixMin;
    private BigDecimal prixMax;

    public ProduitSearchParams() {
        setSort("nom"); // tri par défaut spécifique aux produits
    }
}
```

### `ProduitController.java` — endpoint `/search`

```java
@GetMapping("/search")
public PageResponse<ProduitDto> searchProduits(
        @RequestParam(required = false) String nom,
        @RequestParam(required = false) Long categorieId,
        @RequestParam(required = false) BigDecimal prixMin,
        @RequestParam(required = false) BigDecimal prixMax,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "nom") String sort,
        @RequestParam(defaultValue = "ASC") String direction) {

    ProduitSearchParams params = new ProduitSearchParams();
    params.setNom(nom);
    params.setCategorieId(categorieId);
    params.setPrixMin(prixMin);
    params.setPrixMax(prixMax);
    params.setPage(page);
    params.setSize(size);
    params.setSort(sort);
    params.setDirection(direction);

    return produitService.searchProduits(params);
}
```

---

## 🧪 Tests

### Préalable : Lancer l'application

```bash
./gradlew bootRun
```

### Test 1 : Recherche sans filtre (valeurs par défaut)

```bash
curl "http://localhost:8085/api/produit/search"
```

**Résultat attendu** :
```json
{
  "contenu": [...],
  "page": 0,
  "size": 10,
  "totalElements": 3,
  "totalPages": 1,
  "dernierePage": true
}
```

### Test 2 : Filtre par nom (LIKE insensible à la casse)

```bash
curl "http://localhost:8085/api/produit/search?nom=café"
```

### Test 3 : Filtre par plage de prix

```bash
curl "http://localhost:8085/api/produit/search?prixMin=5.00&prixMax=15.00"
```

### Test 4 : Filtre par catégorie

```bash
curl "http://localhost:8085/api/produit/search?categorieId=1"
```

### Test 5 : Pagination et tri

```bash
# Page 2, 3 résultats par page, triés par prix décroissant
curl "http://localhost:8085/api/produit/search?page=1&size=3&sort=prix&direction=DESC"
```

### Test 6 : Combinaison de filtres

```bash
curl "http://localhost:8085/api/produit/search?nom=café&categorieId=2&prixMin=5&prixMax=20&size=5"
```

---

## 💡 Réutiliser sur un autre endpoint

Pour ajouter la pagination à un nouveau domaine (ex : catégories) :

**1. Créer les params** :
```java
public class CategorieSearchParams extends PageParams {
    private String nom;

    public CategorieSearchParams() {
        setSort("nom");
    }
}
```

**2. Créer la Specification** :
```java
public class CategorieSpecification {
    public static Specification<CategorieEntity> from(CategorieSearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (params.getNom() != null && !params.getNom().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nom")), "%" + params.getNom().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

**3. Étendre le Repository** :
```java
public interface CategorieRepository
        extends JpaRepository<CategorieEntity, Long>,
                JpaSpecificationExecutor<CategorieEntity> {}
```

**4. Service** :
```java
public PageResponse<CategorieDto> searchCategories(CategorieSearchParams params) {
    return new PageResponse<>(
        categorieRepository.findAll(CategorieSpecification.from(params), params.toPageable())
            .map(categorieMapper::toCategorieDto)
    );
}
```

**5. Controller** :
```java
@GetMapping("/search")
public PageResponse<CategorieDto> searchCategories(
        @RequestParam(required = false) String nom,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    // ...
}
```

---

## ✅ Prochaines étapes

1. ✅ Vous savez filtrer dynamiquement avec `Specification`
2. ✅ Vous avez une infrastructure de pagination réutilisable
3. ✅ Vous comprenez les query params avec valeurs par défaut

**Prochaine étape** : Relations — ajouter `@OneToMany` / `@ManyToMany` pour modéliser des commandes ou des tags.

---

*Dernière mise à jour : 2026-09-30*
