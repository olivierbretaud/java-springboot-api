# ÉTAPE 7 : CI et Docker

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Concepts clés](#concepts-clés)
3. [Docker](#docker)
4. [CI avec GitHub Actions](#ci-avec-github-actions)
5. [SonarCloud](#sonarcloud)
6. [Prochaines étapes](#prochaines-étapes)

---

## 🎯 Objectif

**Conteneuriser l'application et automatiser build, tests et analyse qualité à chaque push.**

Après cette étape, vous allez pouvoir :
- ✅ Comprendre un `Dockerfile` multi-stage pour produire une image légère
- ✅ Lancer toute l'application (API + PostgreSQL + MailHog) avec une seule commande
- ✅ Lire un pipeline GitHub Actions et comprendre le rôle de chaque step
- ✅ Interpréter un rapport SonarCloud (couverture, code smells, bugs)

---

## 📖 Concepts clés

### Pourquoi Docker ?

Sans Docker, démarrer l'application sur une nouvelle machine demande : installer Java 17, installer PostgreSQL, créer la base, configurer les variables d'environnement. Avec Docker, tout ça tient dans un `docker-compose up`.

### Pourquoi la CI ?

La CI (Intégration Continue) exécute automatiquement les tests et l'analyse qualité à chaque push. Elle empêche de merger du code cassé ou non couvert.

### Pipeline de cette application

```
Push sur main / develop
        │
        ▼
┌──────────────────────────────────┐
│  GitHub Actions                  │
│                                  │
│  1. Checkout du code             │
│  2. Installer Java 17            │
│  3. Cache Gradle + SonarCloud    │
│  4. Build (sans tests)           │
│  5. Tests + couverture JaCoCo    │
│  6. Analyse SonarCloud           │
└──────────────────────────────────┘
```

---

## 🐳 Docker

### Le `Dockerfile` — build multi-stage

Un build multi-stage utilise **deux images séparées** : une pour compiler, une pour exécuter. L'image finale ne contient que le JRE et le JAR — pas les sources, pas Gradle, pas le JDK.

```dockerfile
# ── Stage 1 : build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copier les fichiers Gradle en premier pour bénéficier du cache
COPY gradlew settings.gradle build.gradle.kts ./
COPY gradle/ gradle/

# Télécharger les dépendances (couche cachée si build.gradle.kts n'a pas changé)
RUN ./gradlew dependencies --no-daemon -q

# Copier les sources et compiler
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2 : runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Pourquoi copier `build.gradle.kts` avant les sources ?**

Docker construit les images couche par couche et met en cache chaque couche. Si `build.gradle.kts` n'a pas changé, Docker réutilise la couche des dépendances téléchargées — le build suivant est beaucoup plus rapide.

```
COPY gradlew build.gradle.kts ...  ← couche A (dépendances)
RUN ./gradlew dependencies         ← couche B (JAR des libs, mis en cache)
COPY src/                          ← couche C (sources, change souvent)
RUN ./gradlew bootJar              ← couche D (recompile seulement si C change)
```

**Utilisateur non-root** : par défaut, les processus dans un container tournent en `root`. C'est un risque de sécurité si le container est compromis. On crée un utilisateur `spring` dédié.

### Le `docker-compose.yml` — stack complète

```yaml
services:

  postgres:
    image: postgres:16-alpine
    container_name: formation-postgres
    environment:
      POSTGRES_DB: formation_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 10

  mailhog:
    image: mailhog/mailhog:latest
    container_name: formation-mailhog
    ports:
      - "1025:1025"   # SMTP
      - "8025:8025"   # Interface web

  api:
    build: .
    container_name: formation-api
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/formation_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      MAIL_HOST: mailhog
      MAIL_PORT: 1025
    ports:
      - "8086:8086"
    restart: unless-stopped

volumes:
  postgres_data:
```

**Points importants :**

- `depends_on` + `condition: service_healthy` : l'API ne démarre qu'une fois PostgreSQL prêt à recevoir des connexions. Sans ça, Spring Boot démarre avant que la base soit disponible et plante.
- `healthcheck` : Docker interroge `pg_isready` toutes les 5 secondes. Ce n'est qu'une fois ce check passé que la condition `service_healthy` est satisfaite.
- `volumes: postgres_data` : les données PostgreSQL survivent à un `docker-compose down`. Un `docker-compose down -v` les supprime.
- Les services communiquent entre eux par leur **nom de service** (`postgres`, `mailhog`) — pas par `localhost`. C'est pourquoi `DB_URL` pointe vers `postgres:5432` et non `localhost:5432`.

### Commandes essentielles

```bash
# Lancer toute la stack (build de l'image API si nécessaire)
docker-compose up --build

# Lancer en arrière-plan
docker-compose up -d

# Voir les logs de l'API
docker-compose logs -f api

# Arrêter sans supprimer les données
docker-compose down

# Arrêter et supprimer les volumes (repart de zéro)
docker-compose down -v

# Reconstruire l'image API après un changement de code
docker-compose build api
docker-compose up -d api
```

---

## ⚙️ CI avec GitHub Actions

### Le fichier `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build-and-analyse:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout du code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0                    # historique complet requis par SonarCloud

      - name: Installation de Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Cache SonarCloud
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar

      - name: Rendre gradlew exécutable
        run: chmod +x gradlew

      - name: Build
        run: ./gradlew build -x test

      - name: Tests, couverture et analyse SonarCloud
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: ./gradlew test jacocoTestReport sonar -Dsonar.branch.name=${{ github.ref_name }}
```

### Rôle de chaque step

| Step | Pourquoi |
|------|----------|
| `checkout` avec `fetch-depth: 0` | SonarCloud analyse le diff entre branches — il lui faut l'historique complet, pas juste le dernier commit |
| `setup-java` | Le runner GitHub est une VM vierge, Java n'y est pas installé |
| `cache Gradle` | Sans cache, Gradle retélécharge ~200 MB de dépendances à chaque run. La clé est un hash des fichiers Gradle : si `build.gradle.kts` change, le cache est invalidé |
| `cache SonarCloud` | Même principe : les données d'analyse SonarCloud sont lourdes à retélécharger |
| `chmod +x gradlew` | Le wrapper Gradle n'a pas le bit exécutable dans le dépôt — il faut l'ajouter avant de pouvoir le lancer |
| `build -x test` | Compile le code sans lancer les tests. Échoue tôt si le code ne compile pas |
| `test jacocoTestReport sonar` | Lance les tests, génère le rapport de couverture XML, envoie tout à SonarCloud |

### Déclencheurs

```yaml
on:
  push:
    branches: [ main, develop ]       # à chaque push sur ces branches
  pull_request:
    branches: [ main, develop ]       # à chaque PR ouverte vers ces branches
```

La CI tourne donc **deux fois** sur une PR : au moment de l'ouverture, puis à chaque nouveau push sur la branche de la PR.

### Secrets GitHub

Les tokens sensibles ne sont jamais écrits en clair dans le YAML. Ils sont configurés dans **Settings → Secrets and variables → Actions** du dépôt.

| Secret | Usage |
|--------|-------|
| `GITHUB_TOKEN` | Fourni automatiquement par GitHub, permet à SonarCloud de décorer les PRs |
| `SONAR_TOKEN` | Token personnel SonarCloud, à générer sur sonarcloud.io |

---

## 🔍 SonarCloud

### Configuration dans `sonar.gradle`

```groovy
sonarqube {
    properties {
        property 'sonar.projectKey', 'olivierbretaud_java-springboot-api'
        property 'sonar.organization', 'olivierbretaud'
        property 'sonar.host.url', 'https://sonarcloud.io'
        property 'sonar.coverage.jacoco.xmlReportPaths', 'build/reports/jacoco/test/jacocoTestReport.xml'
        property 'sonar.java.binaries', 'build/classes/java/main'
    }
}
```

Ce fichier n'est appliqué qu'en CI grâce à la condition dans `build.gradle.kts` :

```kotlin
if (System.getenv("CI") == "true") {
    apply(from = "sonar.gradle")
}
```

Le plugin SonarQube est déclaré dans le bloc `plugins {}` principal (toujours présent), mais sa configuration n'est chargée qu'en CI.

### Ce que SonarCloud analyse

- **Bugs** : code potentiellement incorrect (NullPointerException non gérée, mauvaise comparaison…)
- **Vulnerabilities** : failles de sécurité (injection, exposition de secrets…)
- **Code smells** : code qui fonctionne mais difficile à maintenir (méthodes trop longues, nommage…)
- **Coverage** : pourcentage de lignes couvertes par les tests (fourni par JaCoCo)
- **Duplications** : blocs de code copiés-collés

### Lien vers le rapport

Après chaque run CI, un lien est posté dans le résumé GitHub :

```yaml
- name: Lien vers le rapport SonarCloud
  run: |
    echo "### Rapport SonarCloud" >> $GITHUB_STEP_SUMMARY
    echo "[Voir le rapport](https://sonarcloud.io/summary/new_code?id=olivierbretaud_java-springboot-api&branch=${{ github.ref_name }})" >> $GITHUB_STEP_SUMMARY
```

---

## 💡 Pièges courants

### L'API démarre avant que PostgreSQL soit prêt

```yaml
# ❌ depends_on sans healthcheck : attend juste que le container existe
depends_on:
  - postgres

# ✅ Attend que PostgreSQL accepte des connexions
depends_on:
  postgres:
    condition: service_healthy
```

Sans `service_healthy`, le container PostgreSQL existe mais la base n'est pas encore initialisée. Spring Boot essaie de se connecter, échoue, et le container API s'arrête.

### `DB_URL` pointe vers `localhost` au lieu du service Docker

```yaml
# ❌ localhost dans Docker = le container API lui-même, pas PostgreSQL
DB_URL: jdbc:postgresql://localhost:5432/formation_db

# ✅ Nom du service Docker Compose
DB_URL: jdbc:postgresql://postgres:5432/formation_db
```

### Le cache Gradle est trop agressif

Si la CI échoue avec une erreur de dépendance introuvable après avoir changé `build.gradle.kts`, c'est souvent que le cache Gradle est corrompu. La clé du cache inclut un hash des fichiers Gradle — modifier `build.gradle.kts` invalide automatiquement le cache.

### `fetch-depth: 0` manquant pour SonarCloud

```yaml
# ❌ Checkout superficiel (par défaut depth=1)
- uses: actions/checkout@v4

# ✅ Historique complet pour que SonarCloud puisse comparer les branches
- uses: actions/checkout@v4
  with:
    fetch-depth: 0
```

---

## ✅ Récapitulatif

```
Dockerfile multi-stage
├── Stage builder  → JDK + Gradle → produit le JAR
└── Stage runtime  → JRE seul + JAR → image finale légère et sécurisée

docker-compose.yml
├── postgres   → base de données (healthcheck avant démarrage de l'API)
├── mailhog    → serveur mail de dev (interface web sur :8025)
└── api        → l'application Spring Boot (build depuis le Dockerfile)

CI GitHub Actions
├── Cache Gradle + SonarCloud  → runs rapides
├── Build sans tests           → détecte les erreurs de compilation tôt
└── Tests + JaCoCo + Sonar     → qualité et couverture mesurées à chaque push
```

---

*Dernière mise à jour : 2026-10-23*
