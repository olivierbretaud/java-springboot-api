# Formation Java + Spring Boot + PostgreSQL

API REST complète construite pas à pas : CRUD, pagination, relations JPA, sécurité JWT et tests.

---

## Démarrage rapide

### Prérequis

- Java 17+
- Docker & Docker Compose (PostgreSQL)
- Gradle (wrapper inclus)

### Configuration

Créer un fichier `.env` à la racine du projet avec les variables suivantes :

```env
JWT_SECRET=
JWT_EXPIRATION_MS=
```

> `JWT_EXPIRATION_MS` : durée de validité du token en millisecondes (ex: `86400000` pour 24h).

### Lancer l'application

./gradlew bootRun
```

L'API est disponible sur `http://localhost:8085/api`  
Swagger UI : `http://localhost:8085/api/swagger-ui.html`

---

## Stack technique

| Composant | Version | Rôle |
|-----------|---------|------|
| Java | 17 | Langage principal |
| Spring Boot | 3.5.x | Framework web |
| Spring Security | 3.5.x | Authentification et autorisation |
| Spring Data JPA | 3.5.x | Accès aux données |
| PostgreSQL | 16 | Base de données |
| Liquibase | - | Migrations de schéma |
| Gradle | 8.x | Build system |
| Lombok | - | Réduction du boilerplate |
| SpringDoc OpenAPI | 2.x | Documentation Swagger |
| JJWT | 0.12.6 | Tokens JWT |
| JUnit 5 + Mockito | - | Tests unitaires et de slice |
| TestContainers | - | Tests d'intégration PostgreSQL |

---

## Endpoints

Tous les endpoints (sauf `/auth/login`, `/auth/reset-password`, `/auth/change-password`, Swagger) nécessitent un token JWT.

### Authentification

```
POST   /api/auth/login              → { token: "eyJ..." }
POST   /api/auth/reset-password     → demande de reset par email
POST   /api/auth/change-password    → applique le nouveau mot de passe
GET    /api/auth/profile            → profil de l'utilisateur connecté
```

### Produits

```
GET    /api/produit                 → liste tous les produits
GET    /api/produit/{id}            → un produit par id
POST   /api/produit                 → créer un produit
PUT    /api/produit/{id}            → modifier un produit
DELETE /api/produit/{id}            → supprimer un produit
GET    /api/produit/search          → recherche filtrée + paginée
```

Paramètres de recherche : `nom`, `categorieId`, `prixMin`, `prixMax`, `page`, `size`, `sort`, `direction`.

### Commandes

```
POST   /api/commandes               → créer une commande
GET    /api/commandes/{id}          → consulter une commande
PATCH  /api/commandes/{id}/statut   → changer le statut
```

### Référentiel

```
GET    /api/referentiel/categories  → liste des catégories
GET    /api/referentiel/statuts     → liste des statuts de commande
GET    /api/referentiel/roles       → liste des rôles
```

---

## Exemples cURL

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@formation.com","password":"admin123"}' | jq -r '.token')

# Lister les produits
curl http://localhost:8085/api/produit \
  -H "Authorization: Bearer $TOKEN"

# Recherche filtrée
curl "http://localhost:8085/api/produit/search?nom=café&prixMax=15&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Créer un produit
curl -X POST http://localhost:8085/api/produit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"nom":"Café arabica","prix":12.50,"uniteParPackaging":6,"idCategorie":1}'

# Créer une commande
curl -X POST "http://localhost:8085/api/commandes?utilisateurId=1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"lignes":[{"idProduit":1,"quantite":2}]}'
```

---

## Architecture

```
Controller  →  Service  →  Mapper  →  Repository  →  PostgreSQL
                                            ↑
                                         Entity
```

```
src/main/java/com/formation/
├── security/          ← Spring Security + filtre JWT
├── core/
│   ├── auth/          ← login, reset password
│   ├── produits/      ← CRUD + recherche paginée
│   ├── commandes/     ← création, statuts
│   └── referentiel/   ← catégories, rôles, statuts
└── engine/
    ├── config/        ← JPA Auditing, OpenAPI
    ├── entities/      ← ProduitEntity, CommandeEntity, UtilisateurEntity…
    ├── exceptions/    ← GlobalExceptionHandler
    ├── pagination/    ← PageParams, PageResponse<T>
    └── repositories/
```

Les tables sont gérées par **Liquibase** (16 migrations, `ddl-auto: none`).

---

## Schéma de la base de données

7 tables métier, 6 clés étrangères, 16 migrations Liquibase.

Voir [doc/formation/DATABASE.md](doc/formation/DATABASE.md) pour le diagramme complet, le détail de chaque table et la liste des migrations.

---

## Commandes utiles

```bash
./gradlew bootRun       # lancer l'application
./gradlew test          # tous les tests
./gradlew build         # build du JAR
./gradlew clean         # nettoyer le build
```

---

## Documentation

Les étapes de la formation sont détaillées dans [doc/formation/](doc/formation/) :

| Fichier | Contenu |
|---------|---------|
| [GUIDE_COMPLET.md](doc/formation/GUIDE_COMPLET.md) | Vue d'ensemble et référence |
| [DATABASE.md](doc/formation/DATABASE.md) | Schéma de la base de données |
| [ETAPE_1.md](doc/formation/ETAPE_1.md) | Hello World REST |
| [ETAPE_2.md](doc/formation/ETAPE_2.md) | CRUD Produits + JPA Auditing |
| [ETAPE_3.md](doc/formation/ETAPE_3.md) | Pagination et filtres dynamiques |
| [ETAPE_4.md](doc/formation/ETAPE_4.md) | Relations JPA (Commandes) |
| [ETAPE_5.md](doc/formation/ETAPE_5.md) | Authentification Spring Security + JWT |
| [ETAPE_6.md](doc/formation/ETAPE_6.md) | Pyramide des tests |

---

### Erreurs courantes

```bash
# Port déjà utilisé
lsof -i :8085

# Rebuild complet
./gradlew clean build

# Java version
java -version   # doit afficher Java 17+
```
