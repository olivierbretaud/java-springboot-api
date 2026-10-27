# ÉTAPE 3 : Relations JPA

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Concepts clés](#concepts-clés)
3. [Architecture](#architecture)
4. [Fichiers implémentés](#fichiers-implémentés)
5. [Tests](#tests)
6. [Pièges courants](#pièges-courants)
7. [Prochaines étapes](#prochaines-étapes)

---

## 🎯 Objectif

**Modéliser des relations réalistes entre entités JPA en implémentant un système de commandes.**

Après cette étape, vous allez pouvoir :
- ✅ Créer une commande liée à un utilisateur (`@ManyToOne`)
- ✅ Ajouter des lignes de commande liées à des produits (`@ManyToOne`)
- ✅ Naviguer d'une commande vers ses lignes (`@OneToMany`)
- ✅ Maîtriser `FetchType.LAZY` vs `FetchType.EAGER` et éviter le problème N+1
- ✅ Comprendre les cascades JPA

### Ce que vous allez modéliser

```
┌─────────────────────┐         ┌──────────────────────────┐
│   UtilisateurEntity │         │      ProduitEntity        │
│─────────────────────│         │──────────────────────────│
│ id                  │         │ id                        │
│ nom                 │         │ nom                       │
│ email               │         │ prix                      │
└─────────────────────┘         └──────────────────────────┘
          │                                   │
          │ @ManyToOne                        │ @ManyToOne
          │ "un utilisateur                   │ "un produit
          │  passe N commandes"               │  apparaît dans N lignes"
          │                                   │
          │ N                                 │ N
          ▼                                   ▼
┌─────────────────────┐   1  N  ┌──────────────────────────┐
│   CommandeEntity    │◄────────│   LigneCommandeEntity     │
│─────────────────────│         │──────────────────────────│
│ id                  │         │ id                        │
│ statut              │         │ quantite                  │
│ montantTotal        │         │ prixUnitaire  ← snapshot  │
│ id_utilisateur (FK) │         │ id_commande   (FK)        │
└─────────────────────┘         │ id_produit    (FK)        │
                                └──────────────────────────┘

   CommandeEntity                    LigneCommandeEntity
   ───────────────                   ───────────────────
   @OneToMany                        @ManyToOne          ← propriétaire
   mappedBy = "commande"             @JoinColumn(        (porte le FK)
   cascade = ALL                       name = "id_commande")
   orphanRemoval = true
```

**Qui est propriétaire de la relation ?**
> Toujours le côté qui porte le `@JoinColumn` — ici `LigneCommandeEntity`.
> `mappedBy` sur `CommandeEntity` dit à JPA : *"c'est l'autre côté qui gère la clé étrangère, pas moi."*

---

## 📖 Concepts clés

### 1. `@ManyToOne` — La relation la plus courante

Une **ligne de commande** appartient à une **commande**. Plusieurs lignes → une commande.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_commande", nullable = false)
private CommandeEntity commande;
```

- `@JoinColumn(name = "id_commande")` : nom de la colonne de clé étrangère en base
- `fetch = FetchType.LAZY` : la commande n'est chargée **que si on y accède** (recommandé)

### 2. `@OneToMany` — La collection inverse

Une **commande** a plusieurs **lignes**. On déclare l'inverse de `@ManyToOne` :

```java
@OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
private List<LigneCommandeEntity> lignes = new ArrayList<>();
```

- `mappedBy = "commande"` : c'est le champ dans `LigneCommandeEntity` qui **détient** la relation (pas la commande)
- `cascade = CascadeType.ALL` : toute opération sur la commande se propage aux lignes (persist, merge, remove)
- `orphanRemoval = true` : une ligne retirée de la liste est supprimée en base

> **Règle** : l'entité qui porte le `@JoinColumn` est le **propriétaire** de la relation. C'est toujours le côté `@ManyToOne`.

### 3. `FetchType.LAZY` vs `FetchType.EAGER`

| | `LAZY` | `EAGER` |
|---|---|---|
| Chargement | À la demande (proxy) | Immédiat (JOIN) |
| Par défaut sur | `@OneToMany`, `@ManyToMany` | `@ManyToOne`, `@OneToOne` |
| Recommandé | ✅ Toujours préférer | ⚠️ À éviter |
| Risque | `LazyInitializationException` hors session | Surcharge mémoire |

**Règle** : toujours mettre `fetch = FetchType.LAZY`, y compris sur les `@ManyToOne`. Charger explicitement avec une requête `JOIN FETCH` quand nécessaire.

```java
// ❌ Problème N+1 : 1 requête pour les commandes + N requêtes pour chaque utilisateur
List<CommandeEntity> commandes = commandeRepository.findAll();
commandes.forEach(c -> System.out.println(c.getUtilisateur().getNom())); // N requêtes !

// ✅ Une seule requête avec JOIN FETCH
@Query("SELECT c FROM CommandeEntity c JOIN FETCH c.utilisateur")
List<CommandeEntity> findAllWithUtilisateur();
```

### 4. Cascade et orphanRemoval

Les **cascades** propagent les opérations JPA de parent à enfant :

```java
// CascadeType.PERSIST : persiste les lignes quand on persiste la commande
// CascadeType.MERGE   : met à jour les lignes quand on met à jour la commande
// CascadeType.REMOVE  : supprime les lignes quand on supprime la commande
// CascadeType.ALL     : les trois ci-dessus + REFRESH + DETACH

@OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
private List<LigneCommandeEntity> lignes = new ArrayList<>();
```

```java
// Grâce à CascadeType.ALL, pas besoin de sauvegarder les lignes séparément
CommandeEntity commande = new CommandeEntity();
commande.getLignes().add(new LigneCommandeEntity(...));
commandeRepository.save(commande); // sauvegarde commande + toutes les lignes
```

### 5. `@Transactional` — La session JPA reste ouverte

Sans `@Transactional`, la session JPA est fermée dès la sortie du repository. Accéder ensuite à une collection `LAZY` provoque une `LazyInitializationException`.

```java
// ❌ Sans @Transactional : session fermée après findById()
public CommandeDto getCommandeById(Long id) {
    CommandeEntity commande = commandeRepository.findById(id).get(); // session fermée ici
    commande.getLignes().size(); // BOOM : LazyInitializationException
}

// ✅ Avec @Transactional : session reste ouverte pendant toute la méthode
@Transactional(readOnly = true)
public CommandeDto getCommandeById(Long id) {
    CommandeEntity commande = commandeRepository.findById(id).get(); // session ouverte
    commande.getLignes().size(); // OK : la session est encore active
    return commandeMapper.toCommandeDto(commande);
}
```

**`readOnly = true`** sur les lectures : Hibernate désactive le dirty checking (plus de snapshot des entités en mémoire) → moins de mémoire, légèrement plus rapide.

| Opération | Annotation |
|---|---|
| Lecture simple | `@Transactional(readOnly = true)` |
| Écriture (create, update, delete) | `@Transactional` |
| Rien (méthode sans accès LAZY) | pas nécessaire |

### 6. Snapshot du prix — Pourquoi c'est important

Le prix d'un produit peut changer après la commande. Il faut donc **copier** le prix au moment de la commande :

```java
// ❌ Référence au prix du produit (variable dans le temps)
public BigDecimal getMontant() {
    return produit.getPrix().multiply(BigDecimal.valueOf(quantite));
}

// ✅ Prix figé au moment de la commande
@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal prixUnitaire; // copié depuis produit.getPrix() lors de la création
```

---

## 🏗️ Architecture

### Nouveaux fichiers

```
src/main/java/com/formation/
│
├── engine/entities/
│   ├── CommandeEntity.java             ← Commande (utilisateur, statut FK, lignes)
│   ├── LigneCommandeEntity.java        ← Ligne (produit, quantité, prix snapshot)
│   └── StatutCommandeEntity.java       ← Entité statuts_commande (id, code, libelle)
│
├── engine/repositories/
│   ├── CommandeRepository.java
│   └── StatutCommandeRepository.java
│
├── engine/exceptions/
│   └── CommandeNonSupprimableException.java  ← 409 si statut ≠ EN_ATTENTE
│
└── core/commandes/
    ├── dto/
    │   ├── CommandeCreatePayload.java   ← Entrée création : liste de lignes
    │   ├── LigneCommandePayload.java    ← Entrée ligne : idProduit + quantite
    │   ├── ChangerStatutPayload.java    ← Entrée PATCH : { statut: Long }
    │   ├── CommandeDto.java             ← Sortie commande avec statut { id, label }
    │   ├── LigneCommandeDto.java        ← Sortie ligne avec nom produit + prix
    │   └── CommandeListResponse.java    ← Wrapper GET liste : { commandes: [] }
    ├── mapper/
    │   └── CommandeMapper.java
    ├── service/
    │   ├── ICommandeService.java
    │   └── CommandeService.java
    └── controller/
        └── CommandeController.java

src/main/resources/db/changelog/changes/
    ├── 008-create-commandes.yaml        ← statut VARCHAR(20) initial
    ├── 009-create-lignes-commande.yaml
    ├── 010-create-statuts-commande.yaml ← table statuts_commande (code PK)
    ├── 011-insert-statuts-commande.yaml ← 5 statuts insérés
    ├── 012-migrate-commandes-statut.yaml ← migration vers statut_id FK
    └── 013-add-id-to-statuts-commande.yaml ← ajout colonne id comme nouvelle PK
```

### Modèle de données

```
statuts_commande
  - id       BIGINT PK (séquence)
  - code     VARCHAR(20) UNIQUE  (EN_ATTENTE, CONFIRMEE, EXPEDIEE, LIVREE, ANNULEE)
  - libelle  VARCHAR(100)
    ▲
    │ FK statut_id
    │
utilisateurs (existant)
    │
    │ 1
    │
    ▼ N
commandes
  - id              BIGINT PK
  - statut_id       FK → statuts_commande.id
  - montant_total   DECIMAL(10,2)
  - id_utilisateur  FK → utilisateurs.id
  - date_creation
  - date_modification
    │
    │ 1
    │
    ▼ N
lignes_commande
  - id
  - quantite        INTEGER
  - prix_unitaire   DECIMAL(10,2)   ← snapshot
  - id_commande     FK → commandes.id
  - id_produit      FK → produits.id
```

### Flux de création d'une commande

```
POST /api/commandes
{ lignes: [{ idProduit: 1, quantite: 2 }, ...] }
         ↓
CommandeController.creerCommande(payload, utilisateurId)
         ↓
CommandeService.creerCommande(payload, utilisateurId)
  → charger UtilisateurEntity (vérifier qu'il existe)
  → pour chaque ligne : charger ProduitEntity, copier prix
  → calculer montant_total = Σ (prixUnitaire × quantite)
  → new CommandeEntity(utilisateur, lignes, montant_total)
  → commandeRepository.save(commande)  // cascade sauvegarde les lignes
         ↓
CommandeMapper.toCommandeDto(commande)
         ↓
201 Created { id, statut, montantTotal, lignes: [...] }
```

---

## 💻 Fichiers implémentés

### `StatutCommandeEntity.java`

**Chemin** : `src/main/java/com/formation/engine/entities/StatutCommandeEntity.java`

Les statuts sont stockés en base dans une table dédiée — pas en enum Java. Cela permet de les exposer via le référentiel (`GET /referentiel`) et de les gérer sans recompilation.

```java
@Getter
@Setter
@Entity
@Table(name = "statuts_commande")
public class StatutCommandeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;    // EN_ATTENTE, CONFIRMEE, etc.

    @Column(nullable = false, length = 100)
    private String libelle; // "En attente", "Confirmée", etc.
}
```

### `CommandeEntity.java`

**Chemin** : `src/main/java/com/formation/engine/entities/CommandeEntity.java`

```java
@Getter
@Setter
@Entity
@Table(name = "commandes")
@EntityListeners(AuditingEntityListener.class)
public class CommandeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime date_creation;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime date_modification;

    // ← Relation vers la table statuts_commande (FK statut_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_id", nullable = false)
    private StatutCommandeEntity statut;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal;

    // ← Propriétaire de la relation utilisateur (porte le @JoinColumn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private UtilisateurEntity utilisateur;

    // ← Inverse de la relation lignes (mappedBy pointe vers le champ dans LigneCommandeEntity)
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommandeEntity> lignes = new ArrayList<>();
}
```

### `LigneCommandeEntity.java`

**Chemin** : `src/main/java/com/formation/engine/entities/LigneCommandeEntity.java`

```java
@Getter
@Setter
@Entity
@Table(name = "lignes_commande")
public class LigneCommandeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantite;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire; // snapshot au moment de la commande

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_commande", nullable = false)
    private CommandeEntity commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_produit", nullable = false)
    private ProduitEntity produit;
}
```

### `CommandeCreatePayload.java`

```java
@Getter
@Setter
public class CommandeCreatePayload {

    @NotEmpty
    private List<LigneCommandePayload> lignes;
}
```

### `LigneCommandePayload.java`

```java
@Getter
@Setter
public class LigneCommandePayload {

    @NotNull
    private Long idProduit;

    @NotNull
    @Min(1)
    private Integer quantite;
}
```

### `CommandeDto.java`

**Chemin** : `src/main/java/com/formation/core/commandes/dto/CommandeDto.java`

Le statut est exposé en format `{ id, label }` — cohérent avec les autres référentiels (`CategorieDto`).

```java
@Getter
@Setter
public class CommandeDto {

    private Long id;
    private StatutCommandeDto statut;  // { id: 1, label: "En attente" }
    private BigDecimal montantTotal;
    private LocalDateTime dateCreation;
    private List<LigneCommandeDto> lignes;
}
```

### `CommandeListResponse.java`

**Chemin** : `src/main/java/com/formation/core/commandes/dto/CommandeListResponse.java`

Wrapper pour `GET /commandes?utilisateurId=` — évite de retourner un tableau JSON brut.

```java
@Getter
@Builder
public class CommandeListResponse {
    private List<CommandeDto> commandes;
}
```

### `ChangerStatutPayload.java`

**Chemin** : `src/main/java/com/formation/core/commandes/dto/ChangerStatutPayload.java`

```java
@Getter
@Setter
public class ChangerStatutPayload {

    @NotNull
    private Long statut;  // id numérique du statut dans statuts_commande
}
```

### `LigneCommandeDto.java`

**Chemin** : `src/main/java/com/formation/core/commandes/dto/LigneCommandeDto.java`

```java
@Getter
@Setter
public class LigneCommandeDto {

    private Long id;
    private String nomProduit;   // dénormalisé depuis ProduitEntity
    private Integer quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal montantLigne; // prixUnitaire × quantite, calculé dans le mapper
}
```

### `CommandeMapper.java`

**Chemin** : `src/main/java/com/formation/core/commandes/mapper/CommandeMapper.java`

```java
@Component
@RequiredArgsConstructor
public class CommandeMapper {

    public CommandeDto toCommandeDto(CommandeEntity entity) {
        if (entity == null) return null;

        CommandeDto dto = new CommandeDto();
        dto.setId(entity.getId());
        dto.setStatut(entity.getStatut());
        dto.setMontantTotal(entity.getMontantTotal());
        dto.setDateCreation(entity.getDate_creation());
        dto.setLignes(entity.getLignes().stream()
            .map(this::toLigneCommandeDto)
            .toList());
        return dto;
    }

    private LigneCommandeDto toLigneCommandeDto(LigneCommandeEntity entity) {
        LigneCommandeDto dto = new LigneCommandeDto();
        dto.setId(entity.getId());
        dto.setNomProduit(entity.getProduit().getNom());
        dto.setQuantite(entity.getQuantite());
        dto.setPrixUnitaire(entity.getPrixUnitaire());
        dto.setMontantLigne(
            entity.getPrixUnitaire().multiply(BigDecimal.valueOf(entity.getQuantite()))
        );
        return dto;
    }
}
```

> Le mapper accède à `entity.getProduit().getNom()`. Pour éviter une `LazyInitializationException`, la commande doit être chargée avec ses lignes **et** ses produits dans la même session JPA (via `@Transactional` ou `JOIN FETCH`).

### `ICommandeService.java`

**Chemin** : `src/main/java/com/formation/core/commandes/service/ICommandeService.java`

```java
public interface ICommandeService {
    CommandeDto creerCommande(CommandeCreatePayload payload, @NonNull Long utilisateurId);
    CommandeDto getCommandeById(@NonNull Long id);
    CommandeListResponse listCommandesByUtilisateur(@NonNull Long utilisateurId);
    CommandeDto changerStatut(@NonNull Long id, @NonNull Long statutId);
    void deleteCommande(@NonNull Long id);
}
```

> `@NonNull` (`org.springframework.lang`) sur les paramètres `Long` : les appels `findById(@NonNull ID)` de Spring Data attendent des valeurs non-nulles. L'annotation propage la garantie apportée par `@Valid` et `@RequestParam required=true` en entrée.

### `CommandeRepository.java`

**Chemin** : `src/main/java/com/formation/engine/repositories/CommandeRepository.java`

```java
@Repository
public interface CommandeRepository extends JpaRepository<CommandeEntity, Long> {

    List<CommandeEntity> findByUtilisateurId(Long utilisateurId);

    // Charge commande + lignes + produits en une seule requête SQL
    @Query("SELECT c FROM CommandeEntity c " +
           "JOIN FETCH c.lignes l " +
           "JOIN FETCH l.produit " +
           "WHERE c.id = :id")
    Optional<CommandeEntity> findByIdWithLignesAndProduits(@Param("id") Long id);
}
```

**Pourquoi `JOIN FETCH` ici ?** Sans lui, accéder à `ligne.getProduit().getNom()` dans le mapper déclencherait N requêtes SQL supplémentaires (une par ligne).

### `CommandeService.java` (extrait)

```java
@Service
@RequiredArgsConstructor
@SuppressWarnings("null") // @NotNull Bean Validation garantit la non-nullité en amont
public class CommandeService implements ICommandeService {

    private final CommandeRepository commandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProduitRepository produitRepository;
    private final StatutCommandeRepository statutCommandeRepository;
    private final CommandeMapper commandeMapper;

    @Transactional
    @Override
    public CommandeDto creerCommande(CommandeCreatePayload payload, Long utilisateurId) {
        UtilisateurEntity utilisateur = utilisateurRepository.findById(utilisateurId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        // Statut initial récupéré depuis la base par son code
        StatutCommandeEntity statutInitial = statutCommandeRepository.findByCode("EN_ATTENTE")
            .orElseThrow(() -> new EntityNotFoundException("Statut EN_ATTENTE introuvable"));

        CommandeEntity commande = new CommandeEntity();
        commande.setUtilisateur(utilisateur);
        commande.setStatut(statutInitial);

        for (LigneCommandePayload lignePayload : payload.getLignes()) {
            ProduitEntity produit = produitRepository.findById(lignePayload.getIdProduit())
                .orElseThrow(() -> new EntityNotFoundException("Produit non trouvé : " + lignePayload.getIdProduit()));

            LigneCommandeEntity ligne = new LigneCommandeEntity();
            ligne.setProduit(produit);
            ligne.setQuantite(lignePayload.getQuantite());
            ligne.setPrixUnitaire(produit.getPrix()); // snapshot
            ligne.setCommande(commande);
            commande.getLignes().add(ligne);
        }

        BigDecimal total = commande.getLignes().stream()
            .map(l -> l.getPrixUnitaire().multiply(BigDecimal.valueOf(l.getQuantite())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        commande.setMontantTotal(total);

        return commandeMapper.toCommandeDto(commandeRepository.save(commande));
    }

    @Transactional(readOnly = true)
    @Override
    public CommandeDto getCommandeById(Long id) {
        CommandeEntity commande = commandeRepository.findByIdWithLignesAndProduits(id)
            .orElseThrow(() -> new EntityNotFoundException("Commande non trouvée : " + id));
        return commandeMapper.toCommandeDto(commande);
    }

    @Transactional(readOnly = true)
    @Override
    public CommandeListResponse listCommandesByUtilisateur(Long utilisateurId) {
        List<CommandeDto> commandes = commandeRepository.findByUtilisateurId(utilisateurId)
            .stream()
            .map(commandeMapper::toCommandeDto)
            .toList();
        return CommandeListResponse.builder().commandes(commandes).build();
    }

    @Transactional
    @Override
    public CommandeDto changerStatut(Long id, Long statutId) {
        CommandeEntity commande = commandeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Commande non trouvée : " + id));

        StatutCommandeEntity statut = statutCommandeRepository.findById(statutId)
            .orElseThrow(() -> new EntityNotFoundException("Statut inconnu : " + statutId));

        commande.setStatut(statut);
        return commandeMapper.toCommandeDto(commandeRepository.save(commande));
    }

    @Transactional
    @Override
    public void deleteCommande(Long id) {
        CommandeEntity commande = commandeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Commande non trouvée : " + id));

        if (!"EN_ATTENTE".equals(commande.getStatut().getCode())) {
            throw new CommandeNonSupprimableException(id, commande.getStatut().getCode());
        }

        commandeRepository.delete(commande);
    }
}
```

> `CommandeNonSupprimableException` est catchée par `GlobalExceptionHandler` et retourne un **409 Conflict** avec le message `"La commande 3 ne peut pas être supprimée (statut : CONFIRMEE)"`.

> **`@Transactional(readOnly = true)`** sur les lectures : optimisation qui indique à Hibernate de ne pas suivre les changements d'état des entités (pas de dirty checking), ce qui réduit la consommation mémoire.

### `CommandeController.java`

**Chemin** : `src/main/java/com/formation/core/commandes/controller/CommandeController.java`

```java
@RestController
@RequestMapping("/commandes")
@RequiredArgsConstructor
@SuppressWarnings("null") // @RequestParam et @PathVariable sont required=true → non-null garanti
public class CommandeController {

    private final ICommandeService commandeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeDto creerCommande(
            @Valid @RequestBody CommandeCreatePayload payload,
            @RequestParam Long utilisateurId) {
        return commandeService.creerCommande(payload, utilisateurId);
    }

    @GetMapping("/{id}")
    public CommandeDto getCommandeById(@PathVariable Long id) {
        return commandeService.getCommandeById(id);
    }

    @GetMapping
    public CommandeListResponse listCommandes(@RequestParam Long utilisateurId) {
        return commandeService.listCommandesByUtilisateur(utilisateurId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommande(@PathVariable Long id) {
        commandeService.deleteCommande(id);
    }

    @PatchMapping("/{id}/statut")
    public CommandeDto changerStatut(
            @PathVariable Long id,
            @Valid @RequestBody ChangerStatutPayload payload) {
        return commandeService.changerStatut(id, payload.getStatut());
    }
}
```

| Méthode | URL | Corps | Réponse |
|---------|-----|-------|---------|
| `POST` | `/commandes?utilisateurId=1` | `{ lignes: [...] }` | 201 `CommandeDto` |
| `GET` | `/commandes/{id}` | — | 200 `CommandeDto` |
| `GET` | `/commandes?utilisateurId=1` | — | 200 `{ commandes: [...] }` |
| `DELETE` | `/commandes/{id}` | — | 204 (ou 409 si statut ≠ EN_ATTENTE) |
| `PATCH` | `/commandes/{id}/statut` | `{ "statut": 2 }` | 200 `CommandeDto` |

### Migrations Liquibase

Les migrations pour cette étape couvrent la création des tables **et** la migration des données existantes (passage de `statut VARCHAR` vers `statut_id FK`).

#### `008-create-commandes.yaml` — table initiale avec `statut VARCHAR`

```yaml
- createTable:
    tableName: commandes
    columns:
      - column: { name: id, type: BIGINT, autoIncrement: true, constraints: { primaryKey: true } }
      - column: { name: statut, type: VARCHAR(20), defaultValue: EN_ATTENTE, constraints: { nullable: false } }
      - column: { name: montant_total, type: DECIMAL(10,2), constraints: { nullable: false } }
      - column: { name: id_utilisateur, type: BIGINT, constraints: { nullable: false, foreignKeyName: fk_commandes_utilisateur, references: utilisateurs(id) } }
      - column: { name: date_creation, type: TIMESTAMP, constraints: { nullable: false } }
      - column: { name: date_modification, type: TIMESTAMP, constraints: { nullable: false } }
```

#### `010-create-statuts-commande.yaml` — table de référence

```yaml
- createTable:
    tableName: statuts_commande
    columns:
      - column: { name: code, type: VARCHAR(20), constraints: { primaryKey: true, nullable: false } }
      - column: { name: libelle, type: VARCHAR(100), constraints: { nullable: false } }
- addForeignKeyConstraint:
    baseTableName: commandes
    baseColumnNames: statut
    constraintName: fk_commandes_statut
    referencedTableName: statuts_commande
    referencedColumnNames: code
```

#### `011-insert-statuts-commande.yaml` — données initiales

```yaml
- insert: { tableName: statuts_commande, columns: [{ name: code, value: EN_ATTENTE }, { name: libelle, value: "En attente" }] }
- insert: { tableName: statuts_commande, columns: [{ name: code, value: CONFIRMEE  }, { name: libelle, value: "Confirmée"  }] }
# ... EXPEDIEE, LIVREE, ANNULEE
```

#### `013-add-id-to-statuts-commande.yaml` — ajout de la colonne `id` comme PK numérique

```yaml
# 1. Ajouter id (nullable)
- addColumn: { tableName: statuts_commande, columns: [{ name: id, type: BIGINT }] }
# 2. Peupler avec des valeurs fixes
- sql: |
    UPDATE statuts_commande SET id = 1 WHERE code = 'EN_ATTENTE';
    UPDATE statuts_commande SET id = 2 WHERE code = 'CONFIRMEE';
    UPDATE statuts_commande SET id = 3 WHERE code = 'EXPEDIEE';
    UPDATE statuts_commande SET id = 4 WHERE code = 'LIVREE';
    UPDATE statuts_commande SET id = 5 WHERE code = 'ANNULEE';
# 3. NOT NULL sur id
- addNotNullConstraint: { tableName: statuts_commande, columnName: id, columnDataType: BIGINT }
# 4. Supprimer la FK dépendante de l'ancienne PK
- dropForeignKeyConstraint: { baseTableName: commandes, constraintName: fk_commandes_statut }
# 5. Changer la PK : code → id
- dropPrimaryKey: { tableName: statuts_commande, constraintName: statuts_commande_pkey }
- addUniqueConstraint: { tableName: statuts_commande, columnNames: code, constraintName: uq_statuts_commande_code }
- addPrimaryKey: { tableName: statuts_commande, columnNames: id, constraintName: pk_statuts_commande }
# 6. Séquence pour les futurs inserts
- createSequence: { sequenceName: statuts_commande_id_seq, startValue: 6, incrementBy: 1 }
- addDefaultValue: { tableName: statuts_commande, columnName: id, defaultValueSequenceNext: statuts_commande_id_seq }
```

#### `012-migrate-commandes-statut.yaml` — migration de `statut VARCHAR` vers `statut_id FK`

```yaml
# 1. Nouvelle colonne (nullable pour permettre le remplissage)
- addColumn: { tableName: commandes, columns: [{ name: statut_id, type: BIGINT }] }
# 2. Remplir depuis l'ancienne colonne
- sql:
    sql: UPDATE commandes SET statut_id = (SELECT id FROM statuts_commande WHERE code = commandes.statut)
# 3. Contraintes finales
- addNotNullConstraint: { tableName: commandes, columnName: statut_id, columnDataType: BIGINT }
- addForeignKeyConstraint:
    baseTableName: commandes
    baseColumnNames: statut_id
    constraintName: fk_commandes_statut_id
    referencedTableName: statuts_commande
    referencedColumnNames: id
# 4. Supprimer l'ancienne colonne
- dropColumn: { tableName: commandes, columnName: statut }
```

> **Ordre d'exécution dans le master changelog** : 010 → 011 → 013 → 012. La migration 013 doit créer la colonne `id` avant que 012 puisse créer la FK vers `statuts_commande(id)`.

---

## 🧪 Tests

### Préalable : Lancer l'application

```bash
./gradlew bootRun
```

### Test 1 : Créer une commande

```bash
curl -X POST "http://localhost:8086/api/commandes?utilisateurId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "lignes": [
      { "idProduit": 1, "quantite": 2 },
      { "idProduit": 3, "quantite": 1 }
    ]
  }'
```

**Résultat attendu** :
```json
{
  "id": 1,
  "statut": { "id": 1, "label": "En attente" },
  "montantTotal": 35.97,
  "dateCreation": "2026-10-16T10:00:00",
  "lignes": [
    { "nomProduit": "Café Arabica", "quantite": 2, "prixUnitaire": 12.99, "montantLigne": 25.98 },
    { "nomProduit": "Thé vert",     "quantite": 1, "prixUnitaire": 9.99,  "montantLigne": 9.99  }
  ]
}
```

### Test 2 : Récupérer une commande

```bash
curl "http://localhost:8086/api/commandes/1"
```

### Test 3 : Lister les commandes d'un utilisateur

```bash
curl "http://localhost:8086/api/commandes?utilisateurId=1"
```

**Résultat** : `{ "commandes": [...] }` — tableau enveloppé dans un objet.

### Test 4 : Changer le statut

```bash
curl -X PATCH "http://localhost:8086/api/commandes/1/statut" \
  -H "Content-Type: application/json" \
  -d '{ "statut": 2 }'
```

`statut` est l'**id numérique** du statut dans `statuts_commande` (récupérable via `GET /api/referentiel`).

### Test 5 : Supprimer une commande EN_ATTENTE

```bash
curl -X DELETE "http://localhost:8086/api/commandes/1"
# → 204 No Content
```

**Si le statut n'est pas EN_ATTENTE** :
```bash
# → 409 Conflict
{ "status": 409, "error": "Opération impossible", "message": "La commande 1 ne peut pas être supprimée (statut : CONFIRMEE)" }
```

### Test 6 : Vérifier que le prix est un snapshot

```bash
# 1. Créer une commande avec le produit id=1
# 2. Modifier le prix du produit id=1 via l'API produit
# 3. Récupérer la commande → prixUnitaire doit toujours afficher l'ancien prix
curl "http://localhost:8086/api/commandes/1"
```

### Test 7 : Consulter le référentiel (statuts disponibles)

```bash
curl "http://localhost:8086/api/referentiel"
```

```json
{
  "categories": [...],
  "statutsCommande": [
    { "id": 1, "label": "En attente" },
    { "id": 2, "label": "Confirmée" },
    { "id": 3, "label": "Expédiée" },
    { "id": 4, "label": "Livrée" },
    { "id": 5, "label": "Annulée" }
  ]
}
```

---

## 💡 Pièges courants

### `LazyInitializationException`

```java
// ❌ La session JPA est fermée quand on accède à la collection LAZY
CommandeEntity commande = commandeRepository.findById(1L).get();
// session fermée ici...
commande.getLignes().size(); // BOOM : LazyInitializationException
```

**Solutions** :
- Utiliser `@Transactional` sur la méthode de service
- Ou utiliser une requête `JOIN FETCH` dans le repository

```java
@Query("SELECT c FROM CommandeEntity c JOIN FETCH c.lignes WHERE c.id = :id")
Optional<CommandeEntity> findByIdWithLignes(@Param("id") Long id);
```

### Boucle infinie JSON avec `@JsonManagedReference` / `@JsonBackReference`

Si `CommandeEntity` référence `LigneCommandeEntity` qui référence `CommandeEntity`... Jackson boucle à l'infini.

**Solution** : ne jamais sérialiser les entités directement. Toujours passer par des DTOs.

---

## ✅ Prochaines étapes

1. ✅ Vous savez modéliser des relations `@OneToMany` / `@ManyToOne`
2. ✅ Vous comprenez `FetchType.LAZY` et le problème N+1
3. ✅ Vous maîtrisez les cascades et le snapshot de données

**Prochaine étape** : Authentification & JWT — protéger les endpoints avec Spring Security et des tokens JWT.

---

*Dernière mise à jour : 2026-10-16*
