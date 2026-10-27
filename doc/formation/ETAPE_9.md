# ÉTAPE 9 : Comprendre ce qui se passe en production

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Concepts clés](#concepts-clés)
3. [Spring Boot Actuator](#spring-boot-actuator)
4. [Logs structurés JSON avec MDC](#logs-structurés-json-avec-mdc)
5. [Fichiers implémentés](#fichiers-implémentés)
6. [Pièges courants](#pièges-courants)
7. [Récapitulatif](#récapitulatif)

---

## 🎯 Objectif

**Savoir ce que fait l'application une fois déployée, sans modifier le code métier.**

Après cette étape, vous allez pouvoir :
- ✅ Exposer des endpoints `/health`, `/info` et `/metrics` avec Spring Boot Actuator
- ✅ Sécuriser l'accès aux métriques avec un rôle dédié `ACTUATOR`
- ✅ Produire des logs au format JSON structuré, lisibles par Elasticsearch ou Datadog
- ✅ Enrichir automatiquement chaque ligne de log avec le `userId` et un `requestId`
- ✅ Comprendre pourquoi `MDC.clear()` est obligatoire dans un serveur multi-thread

---

## 📖 Concepts clés

### Pourquoi l'observabilité ?

Sans observabilité, déboguer un incident en production revient à conduire les yeux bandés. Deux outils couvrent les besoins essentiels :

| Besoin | Outil |
|--------|-------|
| L'appli est-elle vivante ? | `/actuator/health` |
| Combien de requêtes par seconde ? | `/actuator/metrics` |
| Quelle version tourne ? | `/actuator/info` |
| Quel utilisateur a déclenché cette erreur ? | Logs structurés + MDC |

### Health indicators

Actuator inspecte automatiquement chaque dépendance détectée dans le classpath (PostgreSQL, mail…) et expose un statut agrégé :

```
UP    = tous les composants fonctionnent
DOWN  = au moins un composant est en erreur
```

Si PostgreSQL tombe, le statut passe à `DOWN` sans que vous écriviez une ligne.

### MDC — Mapped Diagnostic Context

Le MDC est un dictionnaire de chaînes attaché au **thread courant**. Tout ce qu'on y met est automatiquement inclus dans chaque ligne de log produite par ce thread, jusqu'à ce qu'on le nettoie.

```
Thread HTTP 1  → MDC: { userId: "42", requestId: "a3f7..." }
Thread HTTP 2  → MDC: { userId: "17", requestId: "b1e2..." }
```

Deux requêtes simultanées produisent des logs distincts et traçables, sans se mélanger.

### Logs structurés JSON

En production, les logs en texte brut sont difficiles à filtrer. Le format JSON permet aux outils de collecte (Elastic, Datadog, Splunk) de les indexer et de les interroger :

```json
{
  "@timestamp": "2026-10-27T14:32:11.456Z",
  "log.level": "INFO",
  "message": "Commande créée",
  "process.pid": 1,
  "service.name": "formation-api",
  "userId": "42",
  "requestId": "a3f7b8c9-..."
}
```

Spring Boot 3.4+ produit ce format nativement via `logging.structured.format.console: ecs` (Elastic Common Schema) — aucune dépendance supplémentaire n'est nécessaire.

---

## 🔭 Spring Boot Actuator

### Dépendance — `build.gradle.kts`

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
```

### Configuration — `application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics  # n'exposer que le nécessaire
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized     # détails DB/mail visibles seulement si authentifié

info:
  app:
    name: ${spring.application.name}
    description: Formation Spring Boot API
```

**Pourquoi ne pas exposer `*` ?** Certains endpoints Actuator (`env`, `beans`, `heapdump`) révèlent des informations sensibles (variables d'environnement, structure interne). On n'expose que ce dont on a besoin.

### Sécurisation — `SecurityConfig.java`

```java
.requestMatchers("/actuator/health").permitAll()     // sonde Docker/K8s, sans auth
.requestMatchers("/actuator/**").hasRole("ACTUATOR") // métriques : rôle dédié
```

Deux règles distinctes parce que `/health` doit être accessible par les healthchecks Docker (`docker-compose` vérifie que l'API est prête) sans token JWT, tandis que `/metrics` et `/info` ne doivent être lisibles que par un compte technique.

### Rôle ACTUATOR en base — `020-insert-role-actuator.yaml`

```yaml
databaseChangeLog:
  - changeSet:
      id: 020-insert-role-actuator
      author: formation
      changes:
        - insert:
            tableName: roles
            columns:
              - column: { name: code, value: ACTUATOR }
              - column: { name: libelle, value: "Monitoring technique" }
```

Créer ensuite un utilisateur technique (ex. `monitoring@formation.com`) et lui assigner ce rôle. Ce compte ne se connecte jamais à l'interface utilisateur — il sert uniquement aux appels Actuator.

### Ce que retournent les endpoints

**`GET /api/actuator/health`** (sans token)
```json
{ "status": "UP" }
```

**`GET /api/actuator/health`** (avec token ACTUATOR)
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "mail": { "status": "UP" },
    "diskSpace": { "status": "UP", "details": { "free": 52428800 } }
  }
}
```

**`GET /api/actuator/info`** (avec token ACTUATOR)
```json
{
  "app": {
    "name": "formation-api",
    "description": "Formation Spring Boot API"
  }
}
```

**`GET /api/actuator/metrics/http.server.requests`** (avec token ACTUATOR)
```json
{
  "name": "http.server.requests",
  "measurements": [
    { "statistic": "COUNT", "value": 142.0 },
    { "statistic": "TOTAL_TIME", "value": 3.241 },
    { "statistic": "MAX", "value": 0.087 }
  ]
}
```

### Métriques disponibles par défaut

| Métrique | Ce qu'elle mesure |
|----------|-------------------|
| `http.server.requests` | Nombre et durée des requêtes HTTP, par path + status |
| `jvm.memory.used` | Consommation mémoire heap et non-heap |
| `jvm.gc.pause` | Durée des garbage collections |
| `hikaricp.connections.active` | Connexions PostgreSQL en cours |
| `process.cpu.usage` | CPU consommé par le process |

---

## 📋 Logs structurés JSON avec MDC

### Configuration — `application.yml`

```yaml
logging:
  structured:
    format:
      console: ecs   # Elastic Common Schema — JSON natif Spring Boot 3.4+
  level:
    root: INFO
    com.formation: DEBUG
```

### Enrichissement MDC — `JwtAuthenticationFilter.java`

```java
MDC.put("requestId", UUID.randomUUID().toString());
try {
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = utilisateurDetailsService.loadUserByUsername(username);

        if (jwtService.isTokenValid(token, userDetails)) {
            // ... authentication setup ...
            MDC.put("userId", String.valueOf(((UtilisateurEntity) userDetails).getId()));
        }
    }
    filterChain.doFilter(request, response);
} finally {
    MDC.clear(); // obligatoire : les threads sont réutilisés
}
```

**Pourquoi dans ce filtre ?** C'est le point d'entrée de chaque requête HTTP authentifiée. Tout le code qui s'exécute ensuite (service, repository, mapper) bénéficie automatiquement du MDC sans rien modifier.

**Pourquoi `requestId` est défini avant la vérification du token ?** Même les requêtes avec un token invalide produisent des logs (erreurs de validation, tentatives d'intrusion). Le `requestId` permet de relier tous ces logs à la même tentative.

### Ce que ça produit dans la console

Chaque ligne de log inclut automatiquement `userId` et `requestId` :

```json
{
  "@timestamp": "2026-10-27T14:32:11.456Z",
  "log.level": "DEBUG",
  "message": "Commande créée pour l'utilisateur 42",
  "service.name": "formation-api",
  "process.pid": 1,
  "userId": "42",
  "requestId": "a3f7b8c9-1234-5678-abcd-ef0123456789"
}
```

Sans MDC, déboguer une erreur entre 10 requêtes simultanées revient à reconstituer un puzzle sans les pièces de bordure.

---

## 💻 Fichiers implémentés

### `build.gradle.kts` — dépendance ajoutée

```kotlin
// Actuator (métriques, health, info)
implementation("org.springframework.boot:spring-boot-starter-actuator")
```

### `application.yml` — sections ajoutées

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized

info:
  app:
    name: ${spring.application.name}
    description: Formation Spring Boot API

logging:
  structured:
    format:
      console: ecs
  level:
    root: INFO
    com.formation: DEBUG
```

### `SecurityConfig.java` — règles ajoutées

```java
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/actuator/**").hasRole("ACTUATOR")
```

### `020-insert-role-actuator.yaml` — nouvelle migration

Insère le rôle `ACTUATOR` dans la table `roles`.

### `JwtAuthenticationFilter.java` — MDC ajouté

Le filtre injecte maintenant `requestId` (toujours) et `userId` (si token valide) dans le MDC, avec nettoyage garanti dans le bloc `finally`.

---

## 💡 Pièges courants

### `MDC.clear()` manquant — contamination entre requêtes

```java
// ❌ Sans finally : si filterChain.doFilter() lève une exception,
//    MDC.clear() n'est jamais appelé. Le thread suivant hérite des valeurs.
MDC.put("userId", userId);
filterChain.doFilter(request, response);
MDC.clear(); // jamais atteint en cas d'exception

// ✅ finally garantit le nettoyage quoi qu'il arrive
MDC.put("userId", userId);
try {
    filterChain.doFilter(request, response);
} finally {
    MDC.clear();
}
```

### `/actuator/health` accessible vs `/actuator/**` restreint

```java
// ❌ Ordre inversé : la règle générale capture /health avant la règle permissive
.requestMatchers("/actuator/**").hasRole("ACTUATOR")
.requestMatchers("/actuator/health").permitAll()   // jamais atteinte

// ✅ Règle la plus spécifique en premier
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/actuator/**").hasRole("ACTUATOR")
```

Spring Security évalue les règles **dans l'ordre**. La première qui correspond gagne.

### `show-details: always` en production

```yaml
# ❌ Expose les détails DB et système sans authentification
management:
  endpoint:
    health:
      show-details: always

# ✅ Détails visibles seulement si authentifié
management:
  endpoint:
    health:
      show-details: when-authorized
```

`always` est pratique en développement mais révèle des informations sur l'infrastructure en production (type de base, espace disque…).

### Exposer `*` sans réfléchir

```yaml
# ❌ Expose env (variables d'environnement avec secrets), beans, heapdump...
management:
  endpoints:
    web:
      exposure:
        include: "*"

# ✅ N'exposer que ce qui est utile et inoffensif
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
```

---

## ✅ Récapitulatif

```
Spring Boot Actuator
├── /actuator/health   → état de l'appli + dépendances (public pour Docker healthcheck)
├── /actuator/info     → version, nom (rôle ACTUATOR requis)
└── /actuator/metrics  → compteurs JVM, HTTP, DB pool (rôle ACTUATOR requis)

Sécurité
├── Rôle ACTUATOR en base (migration 020)
├── /actuator/health   → permitAll()
└── /actuator/**       → hasRole("ACTUATOR")

Logs structurés JSON (Spring Boot 3.4+ natif)
├── Format ECS → JSON lisible par Elasticsearch, Datadog, Splunk
├── MDC.put("requestId", ...)  → corrèle tous les logs d'une même requête
├── MDC.put("userId", ...)     → identifie l'utilisateur dans chaque ligne
└── MDC.clear() dans finally   → nettoyage obligatoire (threads réutilisés)
```

---

*Dernière mise à jour : 2026-10-27*
