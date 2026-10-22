# Spring Boot — Fonctionnement et dépendances

## Le principe "convention over configuration"

Spring Boot est un framework Java qui **élimine la configuration manuelle** de Spring classique.
Il repose sur 3 mécanismes fondamentaux :

```
┌─────────────────────────────────────────────────────┐
│                   Spring Boot App                   │
│                                                     │
│  @SpringBootApplication                             │
│       │                                             │
│       ├── @ComponentScan     → détecte les beans    │
│       ├── @EnableAutoConfig  → configure auto       │
│       └── @Configuration     → config Java          │
└─────────────────────────────────────────────────────┘
```

- **Auto-configuration** : Spring Boot détecte les librairies présentes dans le classpath et configure automatiquement les beans nécessaires. Si `spring-boot-starter-web` est présent → il configure Tomcat, Jackson, DispatcherServlet automatiquement.
- **Starters** : des dépendances "bundle" qui regroupent tout ce qu'il faut pour une fonctionnalité.
- **Embedded server** : Tomcat est embarqué dans le JAR → `java -jar app.jar` suffit pour démarrer.

---

## Flux de démarrage

```
main() → SpringApplication.run()
    │
    ├── Scan des composants (@Component, @Service, @Repository...)
    ├── Auto-configuration (web, JPA, datasource...)
    ├── Liquibase → exécute les migrations SQL manquantes
    ├── Hibernate → génère/valide le schéma si configuré
    └── Tomcat démarre → l'app est prête
```

---

## Les dépendances du projet

### `spring-boot-starter-web`

Fournit la stack HTTP complète :

- **Tomcat embarqué** (pas besoin de déployer sur un serveur externe)
- **Spring MVC** (`@RestController`, `@GetMapping`, etc.)
- **Jackson** (sérialisation/désérialisation JSON automatique)

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) { ... }
}
```

---

### `spring-boot-starter-data-jpa`

ORM (Object-Relational Mapping) :

- **Hibernate** (implémentation JPA)
- **Spring Data** → génère les requêtes SQL à partir des noms de méthodes

```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByEmail(String email); // SQL généré automatiquement
}
```

---

### `postgresql:42.7.1`

Driver JDBC pour se connecter à PostgreSQL. Configuré via `application.properties` :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass
```

---

### `spring-boot-starter-validation`

Validation des données entrantes avec des annotations Jakarta :

```java
public record CreateUserDto(
    @NotBlank String name,
    @Email    String email,
    @Min(18)  int age
) {}
```

```java
// Dans le controller, @Valid déclenche la validation automatiquement
public ResponseEntity<?> create(@Valid @RequestBody CreateUserDto dto) { ... }
```

---

### `liquibase-core`

**Gestion des migrations de base de données** (versioning du schéma SQL) :

- Chaque modification du schéma = un fichier de migration numéroté
- Liquibase exécute les migrations manquantes au démarrage
- Historique stocké en base dans la table `DATABASECHANGELOG`

```
src/main/resources/db/changelog/
  ├── db.changelog-master.yaml
  ├── 001-create-users-table.sql
  └── 002-add-email-index.sql
```

---

### `springdoc-openapi-starter-webmvc-ui:2.8.9`

Génère automatiquement la **documentation Swagger/OpenAPI** :

- Analyse les contrôleurs et DTOs à la compilation
- Interface web disponible sur `http://localhost:8085/swagger-ui.html`
- Permet de tester les endpoints directement depuis le navigateur

---

### `lombok`

**Génère du code boilerplate** à la compilation (via annotation processor) :

```java
@Data                // getters + setters + equals + hashCode + toString
@Builder             // pattern builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
}
```

---

### `spring-boot-starter-test`

Suite de test complète :

- **JUnit 5** — framework de tests
- **Mockito** — mocking des dépendances
- **AssertJ** — assertions fluides
- **MockMvc** — test des contrôleurs sans démarrer le serveur

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  UserService userService;

    @Test
    void shouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Alice"));
    }
}
```

---

## Gestion des versions : `io.spring.dependency-management`

Ce plugin aligne automatiquement les versions de toutes les dépendances Spring
pour éviter les conflits de versions. C'est pourquoi dans `build.gradle.kts`,
la version n'est pas précisée pour les starters Spring :

```kotlin
// Pas besoin de version ici — gérée par le BOM Spring Boot 3.5.16
implementation("org.springframework.boot:spring-boot-starter-web")
```

Le BOM (Bill of Materials) garantit que toutes les librairies sont compatibles entre elles.
