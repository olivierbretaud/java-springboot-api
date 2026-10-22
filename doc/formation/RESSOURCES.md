# Ressources et Références

## Table des matières

1. [Documentation officielle](#documentation-officielle)
2. [Guides et tutoriels](#guides-et-tutoriels)
3. [Outils recommandés](#outils-recommandés)
4. [Cheat sheets](#cheat-sheets)
5. [Commandes utiles](#commandes-utiles)

---

## Documentation officielle

### Spring Boot
- [Spring Boot Official Docs](https://spring.io/projects/spring-boot)
- [Spring Boot Starters](https://spring.io/projects/spring-boot)
- [Spring Boot Auto Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html)

### Spring Framework
- [Spring Framework Docs](https://spring.io/projects/spring-framework)
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

### JPA/Hibernate
- [Jakarta Persistence (JPA) Docs](https://jakarta.ee/specifications/persistence/3.1/)
- [Hibernate Documentation](https://hibernate.org/orm/documentation/)
- [JPA Annotations Reference](https://docs.jboss.org/hibernate/stable/jpa/reference/en-US/html_single/)

### PostgreSQL
- [PostgreSQL Official Docs](https://www.postgresql.org/docs/)
- [PostgreSQL Query Language](https://www.postgresql.org/docs/current/queries.html)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/)

---

## Guides et tutoriels

### Spring Boot Essentials
1. **Getting Started Guides** : https://spring.io/guides
   - Building a RESTful Web Service
   - Accessing Data with JPA
   - Validating Form Input

2. **Baeldung Tutorials**
   - Spring Boot REST API
   - Spring Data JPA Tutorial
   - Spring Boot with PostgreSQL

3. **Tutorials Point**
   - Spring Boot Tutorial
   - Hibernate Tutorial
   - PostgreSQL Tutorial

---

## Outils recommandés

### IDE
- **IntelliJ IDEA Community** : https://www.jetbrains.com/idea/
  - Excellente support Spring Boot
  - Debugging et inspections
  - Database integration

- **VS Code** : https://code.visualstudio.com/
  - Extension : Extension Pack for Java
  - Extension : Spring Boot Extension Pack
  - Léger et rapide

### Client REST
- **Postman** : https://www.postman.com/
  - GUI complète pour tester les endpoints
  - Collections et environnements
  - Scripts et tests

- **Insomnia** : https://insomnia.rest/
  - Alternativeless lourd que Postman
  - Open source

- **cURL** : Ligne de commande (inclus dans Linux/Mac)
  ```bash
  curl -X GET http://localhost:8085/api/produits
  ```

- **HTTPie** : Alternative élégante à cURL
  ```bash
  http GET localhost:8085/api/produits
  ```

### Database Tools
- **pgAdmin** : Interface web pour PostgreSQL
  ```bash
  docker run --rm -p 5050:80 \
    -e 'PGADMIN_DEFAULT_EMAIL=admin@admin.com' \
    -e 'PGADMIN_DEFAULT_PASSWORD=admin' \
    dpage/pgadmin4
  ```

- **DBeaver** : Client database puissant (gratuit)
  - Inspection des tables
  - Écrire des requêtes SQL
  - Export/Import données

### Build & Dependency Management
- **Gradle** : Système de build utilisé ici
- **Maven** : Alternative à Gradle
- **Gradle Wrapper** (./gradlew) : Utilisé dans ce projet

---

## Cheat sheets

### Spring Boot Annotations

```java
// Application
@SpringBootApplication     // Point d'entrée
@EnableAutoConfiguration   // Active l'auto-config

// Component Scanning
@Component                 // Classe managée par Spring
@Service                   // Service métier
@Repository                // Accès données
@RestController            // Controller REST
@Controller                // Controller MVC

// Dependency Injection
@Autowired                 // Injection par field
@RequiredArgsConstructor   // Constructor injection (Lombok)
@Qualifier                 // Sélectionner un bean spécifique
@Primary                   // Préférer ce bean

// Request Mapping
@RequestMapping            // Préfixe
@GetMapping                // GET
@PostMapping               // POST
@PutMapping                // PUT
@DeleteMapping             // DELETE
@PatchMapping              // PATCH

// Parameters
@PathVariable              // Paramètre dans l'URL : /{id}
@RequestParam              // Paramètre query : ?name=value
@RequestBody               // Body JSON
@RequestHeader             // Header HTTP
@CookieValue               // Cookie

// Validation
@Valid                     // Active la validation
@Validated                 // Valide au niveau classe

// Transactions
@Transactional             // Gère les transactions
@EnableTransactionManagement  // Active les transactions
```

### JPA/Hibernate Annotations

```java
// Entity
@Entity                    // Classe = table
@Table(name = "...")       // Nom de la table

// Identité
@Id                        // Clé primaire
@GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-incrément
@Column                    // Propriétés de colonne

// Relations
@OneToMany                 // 1 → N
@ManyToOne                 // N → 1
@ManyToMany                // N ↔ N
@OneToOne                  // 1 ↔ 1
@JoinColumn                // Clé étrangère
@JoinTable                 // Table de liaison (ManyToMany)

// Callbacks
@PrePersist                // Avant INSERT
@PostPersist               // Après INSERT
@PreUpdate                 // Avant UPDATE
@PostUpdate                // Après UPDATE
@PreRemove                 // Avant DELETE
@PostRemove                // Après DELETE

// Lazy Loading
fetch = FetchType.LAZY     // Chargement tardif
fetch = FetchType.EAGER    // Chargement immédiat
```

### Bean Validation Annotations

```java
// Null/Empty
@NotNull                   // != null
@NotEmpty                  // != null && size > 0
@NotBlank                  // != null && trim().length > 0

// String
@Size(min = 3, max = 50)   // Longueur
@Email                     // Format email
@Pattern(regexp = "...")   // Expression régulière

// Numbers
@Min(value = 0)            // Minimum
@Max(value = 100)          // Maximum
@Positive                  // > 0
@PositiveOrZero            // >= 0
@Negative                  // < 0
@NegativeOrZero            // <= 0
@DecimalMin(value = "0.01")  // Min pour BigDecimal
@DecimalMax(value = "9999")  // Max pour BigDecimal

// Date/Time
@Past                      // Date dans le passé
@PastOrPresent             // Passé ou maintenant
@Future                    // Date dans le futur
@FutureOrPresent           // Futur ou maintenant
```

---

## Commandes utiles

### Gradle

```bash
# Lancer l'app
./gradlew bootRun

# Build JAR
./gradlew build

# Tester
./gradlew test

# Tester avec coverage
./gradlew test jacocoTestReport

# Nettoyer
./gradlew clean

# Voir les dépendances
./gradlew dependencies

# Voir les tâches disponibles
./gradlew tasks

# Refresh des dépendances
./gradlew build --refresh-dependencies
```

### Docker

```bash
# Lancer PostgreSQL
docker-compose up -d

# Arrêter
docker-compose down

# Lancer en premier plan (voir les logs)
docker-compose up

# Voir les logs
docker-compose logs -f postgres

# Supprimer les données (ATTENTION !)
docker-compose down -v

# Redémarrer
docker-compose restart
```

### cURL

```bash
# GET
curl http://localhost:8085/api/produits

# GET avec paramètres
curl "http://localhost:8085/api/produits/search/nom?q=laptop"

# POST
curl -X POST http://localhost:8085/api/produits \
  -H "Content-Type: application/json" \
  -d '{"nom":"Product","prix":99.99,"quantite":5}'

# PUT
curl -X PUT http://localhost:8085/api/produits/1 \
  -H "Content-Type: application/json" \
  -d '{"nom":"Updated","prix":149.99,"quantite":3}'

# DELETE
curl -X DELETE http://localhost:8085/api/produits/1

# Voir les headers
curl -i http://localhost:8085/api/produits

# Verbose (debug)
curl -v http://localhost:8085/api/produits

# Pretty print avec jq
curl http://localhost:8085/api/produits | jq .

# Sauvegarder la réponse
curl http://localhost:8085/api/produits > response.json
```

### Git

```bash
# Cloner le projet
git clone <url>

# Voir le statut
git status

# Ajouter des fichiers
git add .

# Créer un commit
git commit -m "Message"

# Pousser
git push

# Voir l'historique
git log

# Voir les changements
git diff
```

### Java CLI

```bash
# Vérifier la version
java -version

# Compiler
javac Main.java

# Exécuter
java Main

# Jar (lister le contenu)
jar tf application.jar

# Exécuter un JAR
java -jar application.jar
```

---

## Ressources Java

### Tutoriels
- **Oracle Java Tutorials** : https://docs.oracle.com/javase/tutorial/
- **W3Schools Java** : https://www.w3schools.com/java/
- **Java Design Patterns** : https://refactoring.guru/design-patterns/java

### Librairies populaires
- **Lombok** : Réduit le boilerplate
- **MapStruct** : Mapping Entity ↔ DTO
- **JUnit 5** : Framework de test
- **Mockito** : Mock les objets pour les tests
- **Springdoc OpenAPI** : Génère de la documentation Swagger

### Articles recommandés
- REST API Best Practices
- SOLID Principles in Java
- Clean Code
- Design Patterns

---

## Forums et Communautés

### Stack Overflow
- Poser des questions : https://stackoverflow.com/questions/tagged/spring-boot
- Tag : `spring-boot`, `spring-data-jpa`, `postgresql`

### Reddit
- r/java
- r/learnprogramming
- r/webdev

### Twitter
- Suivre les développeurs Spring
- Hashtags : #springboot #java #backend

---

## Livres recommandés

1. **Clean Code** - Robert C. Martin
2. **Effective Java** - Joshua Bloch
3. **Spring in Action** - Craig Walls
4. **Refactoring** - Martin Fowler
5. **Domain-Driven Design** - Eric Evans

---

## Blogs et newsletters

- **Baeldung** : https://www.baeldung.com/
- **DZone** : https://dzone.com/
- **Spring Blog** : https://spring.io/blog
- **Java Advent** : https://www.javaadvent.org/

---

**Version** : 1.0  
**Dernière mise à jour** : 2026-09-16
