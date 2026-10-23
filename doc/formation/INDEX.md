# Formation Java + Spring Boot + PostgreSQL

## Navigation rapide

| Étape | Titre | Concepts clés | Statut |
|-------|-------|---------------|--------|
| 1 | [Hello World API](./ETAPE_1.md) | `@RestController`, `@GetMapping`, `ResponseEntity` | ✅ |
| 2 | [CRUD Produits](./ETAPE_2.md) | `@Entity`, `@Service`, DTO/Payload, Mapper, Liquibase | ✅ |
| 3 | [Filtres et pagination](./ETAPE_3.md) | `Specification`, `PageParams`, `PageResponse<T>` | ✅ |
| 4 | [Relations JPA](./ETAPE_4.md) | `@OneToMany`, `@ManyToOne`, `FetchType`, cascade | ✅ |
| 5 | [Authentification & JWT](./ETAPE_5.md) | Spring Security, JWT, BCrypt, `@WithMockUser` | ✅ |
| 6 | [Tests avancés](./ETAPE_6.md) | pyramide des tests, `@WebMvcTest`, `@DataJpaTest`, TestContainers | ✅ |

---

## Parcours recommandé

```
ETAPE_1 → ETAPE_2 → ETAPE_3 → ETAPE_4 → ETAPE_5 → ETAPE_6
```

Chaque étape s'appuie sur la précédente. Lisez-les dans l'ordre.

---

## Autres documents

| Fichier | Contenu |
|---------|---------|
| [GUIDE_COMPLET.md](./GUIDE_COMPLET.md) | Vue d'ensemble : architecture, concepts, commandes utiles |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Structure des packages et couches de l'application |
| [CONCEPTS.md](./CONCEPTS.md) | Référence des annotations Spring Boot et JPA |
| [RESSOURCES.md](./RESSOURCES.md) | Liens externes, documentation officielle |

---

## Démarrage rapide

# 2. Lancer l'application (port 8085)
./gradlew bootRun

# 3. Vérifier
curl http://localhost:8085/api/hello
```

---

*Dernière mise à jour : 2026-09-30*
