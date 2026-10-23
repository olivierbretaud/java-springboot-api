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
