# Formation Java + Spring Boot + PostgreSQL

## 🚀 Démarrage rapide

### Prérequis
- Java 17+
- Gradle
- Docker & Docker Compose
- Un éditeur (VS Code, IntelliJ, etc.)


### Étape 1 : Lancer l'application
```bash
./gradlew bootRun
```

L'API sera disponible à `http://localhost:8085/api`

---

## 📚 Architecture Spring Boot

### Couches de l'application

```
Controller (REST API)
    ↓
Service (Logique métier)
    ↓
Repository (Accès BD)
    ↓
Entity (Modèle de données)
```

### 1. **Entity** (`Produit.java`)
- Représente une table en base de données
- Annotations JPA : `@Entity`, `@Table`, `@Column`
- Validations : `@NotNull`, `@Positive`, `@NotBlank`
- Lombok : `@Data` génère getters/setters/equals/hashCode

### 2. **DTO** (`ProduitDTO.java`)
- Data Transfer Object = classe pour communiquer avec les clients
- Contient les mêmes validations que l'Entity
- Sépare la structure BD de l'API REST

### 3. **Repository** (`ProduitRepository.java`)
- Interface qui hérite de `JpaRepository<Entity, IdType>`
- Fournit automatiquement les méthodes CRUD (Create, Read, Update, Delete)
- Permet d'écrire des requêtes personnalisées

### 4. **Service** (`ProduitService.java`)
- Contient la logique métier
- Utilise le Repository pour accéder aux données
- Convertit Entity ↔ DTO
- Annotation `@Transactional` pour gérer les transactions BD

### 5. **Controller** (`ProduitController.java`)
- Définit les endpoints REST
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- `@Valid` valide les données entrantes
- Retourne des `ResponseEntity` avec le bon statut HTTP

---

## 🔌 Endpoints disponibles

### Produits
```
GET    /api/produits              → Tous les produits
GET    /api/produits/{id}         → Un produit par ID
POST   /api/produits              → Créer un produit
PUT    /api/produits/{id}         → Mettre à jour
DELETE /api/produits/{id}         → Supprimer

GET    /api/produits/search/nom?q=laptop              → Chercher par nom
GET    /api/produits/search/prix?min=100&max=1000   → Chercher par prix
GET    /api/produits/stock/faible?seuil=10           → Stock faible
```

### Exemple : Créer un produit
```bash
curl -X POST http://localhost:8080/api/produits \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Laptop",
    "description": "Ordinateur portable haute performance",
    "prix": 1299.99,
    "quantite": 5
  }'
```

---

## 🎯 Concepts clés à retenir

### Annotations Spring principales
| Annotation | Utilité |
|-----------|---------|
| `@SpringBootApplication` | Point d'entrée de l'app |
| `@RestController` | Classe qui gère les endpoints |
| `@Service` | Classe avec la logique métier |
| `@Repository` | Accès aux données |
| `@Autowired` / `@RequiredArgsConstructor` | Injection de dépendances |
| `@Transactional` | Gère les transactions BD |

### Annotations JPA
| Annotation | Utilité |
|-----------|---------|
| `@Entity` | Classe = table BD |
| `@Table` | Nom de la table |
| `@Id` | Clé primaire |
| `@GeneratedValue` | Auto-incrément |
| `@Column` | Colonne de la table |
| `@JoinColumn` | Clé étrangère |

### Annotations Validation
| Annotation | Contrôle |
|-----------|----------|
| `@NotNull` | Pas null |
| `@NotBlank` | Pas vide/null |
| `@Positive` | > 0 |
| `@Min(n)` | ≥ n |
| `@Max(n)` | ≤ n |
| `@Email` | Format email |

---

## 🧪 Tester avec cURL ou Postman

### Récupérer tous les produits
```bash
curl http://localhost:8080/api/produits
```

### Récupérer un produit
```bash
curl http://localhost:8080/api/produits/1
```

### Créer un produit
```bash
curl -X POST http://localhost:8080/api/produits \
  -H "Content-Type: application/json" \
  -d '{"nom":"Phone","description":"Smartphone","prix":599.99,"quantite":10}'
```

### Mettre à jour
```bash
curl -X PUT http://localhost:8080/api/produits/1 \
  -H "Content-Type: application/json" \
  -d '{"nom":"Phone XL","description":"New","prix":699.99,"quantite":8}'
```

### Supprimer
```bash
curl -X DELETE http://localhost:8080/api/produits/1
```

---

## 📖 Prochaines étapes

### Phase 2 : Authentification & Sécurité
- Ajouter Spring Security
- JWT tokens
- Roles/Permissions

### Phase 3 : Relations entre entités
- OneToMany (Commandes → Produits)
- ManyToMany (Utilisateurs ↔ Rôles)

### Phase 4 : Tests
- Tests unitaires (JUnit 5)
- Tests d'intégration

### Phase 5 : Déploiement
- Docker de l'app
- CI/CD (GitHub Actions)

---

## 🛠 Structure du projet
```
formation-java/
├── src/
│   ├── main/
│   │   ├── java/com/formation/
│   │   │   ├── FormationApplication.java          (Point d'entrée)
│   │   │   ├── engine/
│   │   │   │   ├── entities/
│   │   │   │   │   └── Categorie.java             (Entité JPA)
│   │   │   │   └── exceptions/
│   │   │   │       └── GlobalExceptionHandler.java (Gestion erreurs globale)
│   │   │   └── step1/
│   │   │       └── HelloController.java           (Endpoints de démonstration)
│   │   └── resources/
│   │       ├── application.yml                    (Configuration)
│   │       └── db/changelog/
│   │           ├── db.changelog-master.yaml       (Master Liquibase)
│   │           └── changes/
│   │               ├── 001-create-categories.yaml (Création table + données)
│   │               └── 002-insert-categories.yaml (Ajout catégories)
│   └── test/
│       └── java/com/formation/
│           └── step1/
│               └── HelloControllerTest.java       (Tests unitaires)
├── build.gradle.kts                               (Dépendances Gradle)
├── docker-compose.yml                             (PostgreSQL)
└── README.md
```

---

## ⚡ Commandes utiles

```bash
# Lancer l'app
./gradlew bootRun

# Build JAR
./gradlew build

# Tester
./gradlew test

# Nettoyer
./gradlew clean

# Afficher les dépendances
./gradlew dependencies

# Appliquer les migrations Liquibase (sans redémarrer l'app)
./gradlew update
```

---


### Erreur : "Address already in use" port 8080
```bash
# Trouver le processus utilisant le port
lsof -i :8080

# Arrêter l'app précédente ou changer le port dans application.yml
```

### Erreur de compilation
```bash
# Rebuild complète
./gradlew clean build

# Vérifier que Java 17+ est installé
java -version
```

---

✨ **Bonne formation !** N'hésitez pas à explorer, modifier, et expérimenter.
