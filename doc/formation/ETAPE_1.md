# ÉTAPE 1 : Hello World API

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Prérequis](#prérequis)
3. [Concepts clés](#concepts-clés)
4. [Étapes détaillées](#étapes-détaillées)
5. [Code à créer](#code-à-créer)
6. [Tests](#tests)
7. [Débogage](#débogage)
8. [Exercices](#exercices)
9. [Prochaines étapes](#prochaines-étapes)

---

## 🎯 Objectif

**Créer notre premier endpoint REST et comprendre les concepts de base.**

Après cette étape, vous allez pouvoir :
- ✅ Comprendre la structure d'une API REST
- ✅ Créer des endpoints avec Spring Boot
- ✅ Tester une API avec cURL
- ✅ Comprendre les annotations `@RestController`, `@GetMapping`, `ResponseEntity`

---

## 📋 Prérequis

- Java 17+ installé
- Gradle installé
- Docker et Docker Compose installés
- PostgreSQL en cours d'exécution (voir ci-dessous)
- VS Code ou IntelliJ (optionnel)

### Vérifier les prérequis

```bash
# Vérifier Java
java -version
# Résultat attendu : 17.x.x ou supérieur

# Vérifier Gradle
gradle --version
# Résultat attendu : 8.x

# Vérifier Docker
docker --version
docker-compose --version
```

### Lancer PostgreSQL

```bash
# Depuis la racine du projet
docker-compose up -d

# Vérifier que PostgreSQL est lancé
docker ps | grep postgres
# Vous devriez voir un conteneur postgres en cours d'exécution
```

---

## 📖 Concepts clés

### 1. @RestController

Indique à Spring que cette classe est un **Controller** qui gère les requêtes HTTP et retourne du **JSON**.

```java
@RestController  // ← Important !
public class HelloController {
    // Cette classe traite les requêtes HTTP
}
```

**Différences importantes** :
- `@RestController` → retourne JSON/XML (API REST)
- `@Controller` → retourne des pages HTML (MVC traditionnel)

### 2. @RequestMapping

Définit un **préfixe pour tous les endpoints** de la classe.

```java
@RestController
@RequestMapping("/step1")  // ← Tous les endpoints commencent par /api/step1
public class HelloController {
    @GetMapping("/hello")  // URL complète : /api/step1/hello
    public String hello() { ... }
}
```

### 3. @GetMapping

Crée un endpoint **GET** (récupérer des données).

```java
@GetMapping("/hello")
public ResponseEntity<String> hello() {
    return ResponseEntity.ok("Hello World!");
}
```

**Les autres mappings** :
```java
@PostMapping("/users")      // Créer une ressource
@PutMapping("/users/{id}")  // Remplacer une ressource
@DeleteMapping("/users/{id}")// Supprimer une ressource
@PatchMapping("/users/{id}") // Modifier partiellement
```

### 4. @RequestParam

Récupère un **paramètre de la query string** (`?nom=Alice`).

```java
@GetMapping("/greet")
public String greet(@RequestParam(defaultValue = "Monde") String nom) {
    return "Bonjour " + nom + "!";
}

// URL : /api/step1/greet?nom=Alice
// Résultat : "Bonjour Alice!"
```

**Options** :
```java
@RequestParam String nom                           // Obligatoire
@RequestParam(defaultValue = "Monde") String nom   // Optionnel avec défaut
@RequestParam(required = false) String nom         // Optionnel
@RequestParam(name = "n") String nom               // Nom différent dans l'URL
```

### 5. ResponseEntity

Retourne une réponse HTTP complète : **status + headers + body**.

```java
// Succès
ResponseEntity.ok(objet);                    // 200 OK
ResponseEntity.created(uri).body(objet);     // 201 Created

// Erreur
ResponseEntity.badRequest().body("Erreur");  // 400 Bad Request
ResponseEntity.status(HttpStatus.NOT_FOUND).build();  // 404 Not Found

// Vide
ResponseEntity.noContent().build();          // 204 No Content
```

**Alternative simple** :
```java
@GetMapping("/simple")
public String simple() {
    return "Hello!";  // Retourne 200 OK automatiquement
}
```

### 6. Spring Bean Validation

Les annotations comme `@NotNull`, `@NotBlank` valident automatiquement les données.

```java
@GetMapping("/user")
public ResponseEntity<String> getUser(
    @Valid @RequestBody UserDTO user) {
    // Si user.nom est null/vide, Spring génère une erreur 400
    return ResponseEntity.ok(user.getNom());
}

class UserDTO {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;
}
```

---

## 🔧 Étapes détaillées

### Étape 1 : Vérifier que l'application démarre

```bash
# Depuis la racine du projet
./gradlew bootRun

# Vous devriez voir :
# ...
# Started FormationApplication in X.XXX seconds
# Tomcat started on port(s): 8085 (http) with context path '/api'
```

Si cela échoue, consultez [Débogage](#débogage).

### Étape 2 : Vérifier les fichiers existants

L'étape 1 a déjà un controller créé pour vous. Consultez :

```bash
cat src/main/java/com/formation/step1/HelloController.java
```

### Étape 3 : Tester les endpoints

Ouvrez un **nouveau terminal** (gardez `./gradlew bootRun` en cours) :

```bash
# Test 1 : Hello simple
curl http://localhost:8085/api/step1/hello

# Test 2 : Avec paramètre
curl "http://localhost:8085/api/step1/greet?nom=Alice"

# Test 3 : Objet JSON
curl http://localhost:8085/api/step1/info
```

Vous devriez recevoir des réponses JSON.

---

## 💻 Code à créer

### Fichier principal : HelloController.java

**Chemin** : `src/main/java/com/formation/step1/HelloController.java`

```java
package com.formation.step1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController                    // ← Classe de controller REST
@RequestMapping("/step1")          // ← Préfixe : /api/step1
public class HelloController {

    // ==================== ENDPOINT 1 ====================
    // Simple texte de réponse
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello World!");
    }


    // ==================== ENDPOINT 2 ====================
    // Avec paramètre
    @GetMapping("/greet")
    public ResponseEntity<String> greet(
            @RequestParam(defaultValue = "Monde") String nom) {
        return ResponseEntity.ok("Bonjour " + nom + "!");
    }


    // ==================== ENDPOINT 3 ====================
    // Retourner un objet JSON
    @GetMapping("/info")
    public ResponseEntity<ApiResponse> getInfo() {
        ApiResponse response = new ApiResponse(
            "Application de formation",
            "v1.0",
            "Spring Boot + PostgreSQL"
        );
        return ResponseEntity.ok(response);
    }


    // ==================== CLASSE INTERNE ====================
    // Structure pour le JSON de l'endpoint /info
    static class ApiResponse {
        public String title;
        public String version;
        public String description;

        public ApiResponse(String title, String version, String description) {
            this.title = title;
            this.version = version;
            this.description = description;
        }
    }
}
```

### Explication ligne par ligne

#### Package et imports
```java
package com.formation.step1;  // Chemin du package
```

#### Annotations de classe
```java
@RestController  // Dit à Spring : "Je suis un controller REST"
                 // Les méthodes retournent du JSON, pas des pages HTML
@RequestMapping("/step1")  // Préfixe pour TOUS les endpoints de cette classe
                           // URL : /api/step1 + @GetMapping("/hello") = /api/step1/hello
```

#### Endpoint 1 : GET /hello
```java
@GetMapping("/hello")  // Crée un endpoint GET /api/step1/hello
public ResponseEntity<String> hello() {
    return ResponseEntity.ok("Hello World!");  // Status 200 + "Hello World!"
}
```

**Requête** : `curl http://localhost:8085/api/step1/hello`  
**Réponse** : `"Hello World!"`

#### Endpoint 2 : GET /greet avec paramètre
```java
@GetMapping("/greet")
public ResponseEntity<String> greet(
        @RequestParam(defaultValue = "Monde") String nom
        // ↓
        // Récupère le paramètre ?nom=...
        // Si absent, utilise "Monde"
) {
    return ResponseEntity.ok("Bonjour " + nom + "!");
}
```

**Requêtes possibles** :
- `curl http://localhost:8085/api/step1/greet` → `"Bonjour Monde!"`
- `curl http://localhost:8085/api/step1/greet?nom=Alice` → `"Bonjour Alice!"`

#### Endpoint 3 : GET /info retournant un objet JSON
```java
@GetMapping("/info")
public ResponseEntity<ApiResponse> getInfo() {
    ApiResponse response = new ApiResponse(
        "Application de formation",
        "v1.0",
        "Spring Boot + PostgreSQL"
    );
    return ResponseEntity.ok(response);
    // Spring sérialise automatiquement response en JSON
}
```

**Requête** : `curl http://localhost:8085/api/step1/info`  
**Réponse** :
```json
{
  "title": "Application de formation",
  "version": "v1.0",
  "description": "Spring Boot + PostgreSQL"
}
```

---

## 🧪 Tests

### Test 1 : Endpoint simple

```bash
curl http://localhost:8085/api/step1/hello
```

**Résultat attendu** :
```
"Hello World!"
```

### Test 2 : Avec paramètre

```bash
curl "http://localhost:8085/api/step1/greet?nom=Alice"
```

**Résultat attendu** :
```
"Bonjour Alice!"
```

**Note** : Les guillemets sont importants car `&` a un sens spécial dans le shell.

### Test 3 : Objet JSON

```bash
curl http://localhost:8085/api/step1/info
```

**Résultat attendu** :
```json
{
  "title": "Application de formation",
  "version": "v1.0",
  "description": "Spring Boot + PostgreSQL"
}
```

### Test 4 : Pretty print avec jq

```bash
curl http://localhost:8085/api/step1/info | jq .
```

**Résultat** : JSON bien formaté

### Test 5 : Voir les headers HTTP

```bash
curl -i http://localhost:8085/api/step1/hello
```

**Résultat** : Status HTTP + headers + body

---

## 🔍 Débogage

### Erreur : "Connection refused" (PostgreSQL)

```bash
# Vérifier que PostgreSQL tourne
docker ps | grep postgres

# Si aucun résultat, relancer
docker-compose up -d

# Vérifier les logs
docker-compose logs postgres
```

### Erreur : "Address already in use" (port 8085)

```bash
# Trouver le processus utilisant le port
lsof -i :8085

# Arrêter le processus (ou le relancer)
kill -9 <PID>

# Ou changer le port dans application.yml
```

### Erreur : "Failed to load class org.postgresql.Driver"

```bash
# Rebuild complète
./gradlew clean build

# Vérifier que PostgreSQL est bien dans build.gradle.kts
cat build.gradle.kts | grep postgresql
```

### L'application ne démarre pas

```bash
# Voir les logs détaillés
./gradlew bootRun --info

# Ou vérifier la syntaxe
./gradlew clean && ./gradlew build
```

### Les endpoints retournent 404

**Vérifier** :
1. L'application est lancée avec `./gradlew bootRun`
2. L'URL est correcte : `http://localhost:8085/api/step1/hello`
3. Le port est 8085 (vérifier dans application.yml)
4. Le contexte est `/api` (vérifier dans application.yml)

---

## 💡 Exercices

### Exercice 1 : Créer un endpoint /math

**Objectif** : Créer un endpoint qui retourne le résultat d'une addition.

**Endpoint** : `GET /api/step1/math`

**Réponse attendue** :
```json
{
  "operation": "2 + 2",
  "result": 4
}
```

**Indice** : Créez une classe interne similaire à `ApiResponse`.

### Exercice 2 : Endpoint avec paramètres

**Objectif** : Créer un endpoint qui additionne deux nombres.

**Endpoint** : `GET /api/step1/add?a=5&b=3`

**Réponse attendue** :
```
"5 + 3 = 8"
```

**Indice** : Utilisez deux `@RequestParam`.

### Exercice 3 : Endpoint dynamique

**Objectif** : Créer un endpoint qui salue une personne par son nom.

**Endpoint** : `GET /api/step1/user/{name}` (paramètre dans l'URL, pas la query)

**Réponse attendue** :
```
"Utilisateur: Alice"
```

**Indice** : Utilisez `@PathVariable` à la place de `@RequestParam`.

**Solution** :
```java
@GetMapping("/user/{name}")
public ResponseEntity<String> getUser(@PathVariable String name) {
    return ResponseEntity.ok("Utilisateur: " + name);
}
```

### Exercice 4 : Objet complexe

**Objectif** : Créer un objet `Person` avec `nom`, `age`, `email`.

**Endpoint** : `GET /api/step1/person`

**Réponse attendue** :
```json
{
  "nom": "Alice",
  "age": 30,
  "email": "alice@example.com"
}
```

---

## ✅ Prochaines étapes

Quand vous êtes à l'aise avec cette étape :

1. ✅ Vous comprenez `@RestController`, `@GetMapping`, `ResponseEntity`
2. ✅ Vous pouvez créer des endpoints simples
3. ✅ Vous pouvez retourner du JSON
4. ✅ Vous pouvez passer des paramètres

**Prochaine étape** : [ETAPE_2.md](./ETAPE_2.md) - **CRUD Produits**

À l'étape 2, vous allez :
- Créer une Entity (table en BD)
- Créer un Repository (accès BD)
- Créer un Service (logique métier)
- Implémenter les 4 verbes HTTP (GET, POST, PUT, DELETE)

---

**Version** : 1.0  
**Dernière mise à jour** : 2026-09-16
