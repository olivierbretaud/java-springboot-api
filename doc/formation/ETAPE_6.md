# ÉTAPE 6 : Tests avancés

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Concepts clés](#concepts-clés)
3. [Architecture](#architecture)
4. [Fichiers implémentés](#fichiers-implémentés)
5. [Tests](#tests)
6. [Prochaines étapes](#prochaines-étapes)

---

## 🎯 Objectif

**Comprendre la pyramide des tests et écrire chaque couche au bon niveau.**

Après cette étape, vous allez pouvoir :
- ✅ Différencier tests unitaires, tests de slice et tests d'intégration
- ✅ Tester un Controller avec `@WebMvcTest` sans démarrer toute l'application
- ✅ Tester un Repository avec `@DataJpaTest` sur une vraie base de données
- ✅ Remplacer H2 par PostgreSQL dans les tests avec **TestContainers**
- ✅ Lire les tests existants du projet et comprendre pourquoi chaque approche a été choisie

### La pyramide des tests

```
                    ┌──────────────┐
                    │  Intégration │  peu nombreux, lents, coûteux
                    │  (E2E / API) │  TestContainers + Spring complet
                    └──────┬───────┘
                    ┌──────┴────────────┐
                    │   Slice tests     │  ciblés, contexte partiel
                    │  @WebMvcTest      │  @WebMvcTest, @DataJpaTest
                    │  @DataJpaTest     │
                    └──────┬────────────┘
          ┌────────────────┴──────────────────────┐
          │           Tests unitaires             │  nombreux, rapides, fiables
          │  @ExtendWith(MockitoExtension.class)  │  pas de Spring, Mockito pur
          │  new Mapper(), new Specification()    │
          └───────────────────────────────────────┘
```

**Règle** : la majorité des tests doit être en bas de la pyramide. Plus on monte, plus on teste peu mais de bout en bout.

---

## 📖 Concepts clés

### 1. Tests unitaires — pas de Spring, pas de base de données

Pour les classes sans dépendances Spring (Mapper, Specification, logique métier isolée) :

```java
class ProduitMapperTest {

    private ProduitMapper produitMapper;

    @BeforeEach
    void setUp() {
        produitMapper = new ProduitMapper(new EntityMapper()); // instanciation directe
    }

    @Test
    void toProduitDto_mappe_correctement_entity_vers_dto() {
        ProduitEntity entity = new ProduitEntity();
        entity.setId(1L);
        entity.setNom("Banane");

        ProduitDto dto = produitMapper.toProduitDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNom()).isEqualTo("Banane");
    }
}
```

Pas d'annotation de classe. Démarrage immédiat, aucune infrastructure requise.

### 2. Tests de service — Mockito pur avec `@ExtendWith`

Pour tester un Service qui dépend d'un Repository, on **mocke** le Repository :

```java
@ExtendWith(MockitoExtension.class)          // active Mockito, pas Spring
class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository; // faux repository

    @Mock
    private ProduitMapper produitMapper;

    @InjectMocks
    private ProduitService produitService;       // le vrai service, injecté avec les mocks

    @Test
    void createProduit_leve_exception_si_categorie_inexistante() {
        ProduitCreatePayload request = new ProduitCreatePayload();
        request.setIdCategorie(99L);

        when(categorieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> produitService.createProduit(request));
    }
}
```

**Avantages** : très rapide, teste la logique métier sans la base de données ni Spring.

### 3. `@WebMvcTest` — tester le Controller seul

Démarre uniquement la couche Web (serialisation JSON, validation, routing HTTP). Le service est **mocké**.

```java
@WebMvcTest(ProduitController.class)   // charge UNIQUEMENT le controller + Spring MVC
class ProduitControllerTest {

    @Autowired
    private MockMvc mockMvc;           // client HTTP simulé

    @MockitoBean                       // Spring Boot 3.4+ (remplace @MockBean)
    private IProduitService produitService;

    @Test
    void createProduit_retourne_400_si_nom_vide() throws Exception {
        mockMvc.perform(post("/api/produit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"\",\"prix\":12.50,\"idCategorie\":1}"))
                .andExpect(status().isBadRequest()); // validation Bean Validation
    }

    @Test
    void getProduitById_retourne_404_si_inexistant() throws Exception {
        when(produitService.getProduitById(99L))
                .thenThrow(new EntityNotFoundException("Produit introuvable : 99"));

        mockMvc.perform(get("/api/produit/99"))
                .andExpect(status().isNotFound()); // GlobalExceptionHandler
    }
}
```

**Ce qu'on teste ici** : routing, validation des payloads, codes HTTP, sérialisation JSON, `GlobalExceptionHandler`.  
**Ce qu'on ne teste pas** : la logique métier (c'est le rôle des tests de service).

### 4. `@DataJpaTest` — tester le Repository avec une vraie base

Démarre uniquement la couche JPA (entités, repositories, migrations Liquibase). Pas de controllers, pas de services.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // utilise notre config
class CommandeRepositoryTest {

    @Autowired
    private TestEntityManager em;      // utilitaire pour préparer les données de test

    @Autowired
    private CommandeRepository commandeRepository;

    @Test
    void findByUtilisateurId_retourne_commandes_de_cet_utilisateur() {
        UtilisateurEntity utilisateur = em.persist(unUtilisateur());
        em.persist(uneCommande(utilisateur));
        em.flush();

        List<CommandeEntity> commandes = commandeRepository.findByUtilisateurId(utilisateur.getId());

        assertThat(commandes).hasSize(1);
    }
}
```

**Ce qu'on teste ici** : requêtes `@Query`, contraintes FK en base, cascade JPA, comportement réel de Hibernate.

### 5. TestContainers — PostgreSQL réel dans les tests

Par défaut, `@DataJpaTest` utilise une base H2 en mémoire. Problème : H2 ne se comporte pas exactement comme PostgreSQL (types, fonctions SQL, contraintes).

**TestContainers** lance un vrai container Docker PostgreSQL pendant les tests :

```xml
<!-- build.gradle.kts -->
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:postgresql")
testImplementation("org.testcontainers:junit-jupiter")
```

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CommandeRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("formation_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",    postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // Les tests s'exécutent contre un vrai PostgreSQL
}
```

**`static`** sur le container = il est démarré une seule fois pour toute la classe de test (plus rapide).  
**`@DynamicPropertySource`** = injecte l'URL du container dans la config Spring au démarrage.

---

## 🏗️ Architecture

### Répertoire des tests

```
src/test/java/com/formation/
│
├── engine/
│   ├── utils/EntityMapperTest.java              ← Unitaire (new EntityMapper())
│   └── exceptions/GlobalExceptionHandlerTest.java ← Unitaire
│
├── core/produits/
│   ├── mapper/ProduitMapperTest.java            ← Unitaire (new ProduitMapper())
│   ├── specification/ProduitSpecificationTest.java ← Unitaire (Mockito criteria)
│   ├── service/ProduitServiceTest.java          ← @ExtendWith(MockitoExtension)
│   └── controller/ProduitControllerTest.java    ← @WebMvcTest
│
└── core/commandes/                              ← Nouveaux tests ETAPE_5
    ├── mapper/CommandeMapperTest.java           ← Unitaire
    ├── service/CommandeServiceTest.java         ← @ExtendWith(MockitoExtension)
    ├── controller/CommandeControllerTest.java   ← @WebMvcTest
    └── repository/CommandeRepositoryTest.java   ← @DataJpaTest + TestContainers
```

### Quelle annotation pour quel besoin ?

```
Besoin                              Annotation                  Vitesse
─────────────────────────────────────────────────────────────────────────
Tester mapper / specification       aucune (new MaClasse())     ⚡⚡⚡
Tester logique service              @ExtendWith(Mockito)        ⚡⚡⚡
Tester routing / validation HTTP    @WebMvcTest                 ⚡⚡
Tester requêtes JPA / contraintes   @DataJpaTest + Testcontainers ⚡
Tester tout de bout en bout         @SpringBootTest             🐢
```

---

## 💻 Fichiers implémentés

### `CommandeControllerTest.java`

```java
@WebMvcTest(CommandeController.class)
class CommandeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICommandeService commandeService;

    @Test
    void creerCommande_retourne_201() throws Exception {
        CommandeDto dto = new CommandeDto();
        dto.setId(1L);
        dto.setStatut(StatutCommande.EN_ATTENTE);
        dto.setMontantTotal(new BigDecimal("25.98"));

        when(commandeService.creerCommande(any(), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/commandes")
                .param("utilisateurId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lignes\":[{\"idProduit\":1,\"quantite\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.montantTotal").value(25.98));
    }

    @Test
    void creerCommande_retourne_400_si_lignes_vides() throws Exception {
        mockMvc.perform(post("/api/commandes")
                .param("utilisateurId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lignes\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerCommande_retourne_404_si_utilisateur_inexistant() throws Exception {
        when(commandeService.creerCommande(any(), anyLong()))
                .thenThrow(new EntityNotFoundException("Utilisateur non trouvé"));

        mockMvc.perform(post("/api/commandes")
                .param("utilisateurId", "99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lignes\":[{\"idProduit\":1,\"quantite\":1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changerStatut_retourne_200() throws Exception {
        CommandeDto dto = new CommandeDto();
        dto.setId(1L);
        dto.setStatut(StatutCommande.CONFIRMEE);

        when(commandeService.changerStatut(1L, StatutCommande.CONFIRMEE)).thenReturn(dto);

        mockMvc.perform(patch("/api/commandes/1/statut")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"CONFIRMEE\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"));
    }
}
```

### `CommandeServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class CommandeServiceTest {

    @Mock private CommandeRepository commandeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ProduitRepository produitRepository;
    @Mock private CommandeMapper commandeMapper;

    @InjectMocks
    private CommandeService commandeService;

    @Test
    void creerCommande_calcule_montant_total() {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        utilisateur.setId(1L);

        ProduitEntity produit = new ProduitEntity();
        produit.setId(1L);
        produit.setPrix(new BigDecimal("12.99"));

        LigneCommandePayload ligne = new LigneCommandePayload();
        ligne.setIdProduit(1L);
        ligne.setQuantite(2);

        CommandeCreatePayload payload = new CommandeCreatePayload();
        payload.setLignes(List.of(ligne));

        CommandeEntity commandeSauvegardee = new CommandeEntity();
        CommandeDto dto = new CommandeDto();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(commandeMapper.toCommandeDto(any())).thenReturn(dto);

        commandeService.creerCommande(payload, 1L);

        // Vérifie que save a été appelé avec le bon montant total
        ArgumentCaptor<CommandeEntity> captor = ArgumentCaptor.forClass(CommandeEntity.class);
        verify(commandeRepository).save(captor.capture());
        assertThat(captor.getValue().getMontantTotal())
                .isEqualByComparingTo(new BigDecimal("25.98")); // 12.99 × 2
    }

    @Test
    void creerCommande_leve_exception_si_utilisateur_inexistant() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commandeService.creerCommande(new CommandeCreatePayload(), 99L));
    }

    @Test
    void creerCommande_snapshot_le_prix_du_produit() {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        ProduitEntity produit = new ProduitEntity();
        produit.setId(1L);
        produit.setPrix(new BigDecimal("9.99"));

        LigneCommandePayload lignePayload = new LigneCommandePayload();
        lignePayload.setIdProduit(1L);
        lignePayload.setQuantite(1);

        CommandeCreatePayload payload = new CommandeCreatePayload();
        payload.setLignes(List.of(lignePayload));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(commandeMapper.toCommandeDto(any())).thenReturn(new CommandeDto());

        commandeService.creerCommande(payload, 1L);

        ArgumentCaptor<CommandeEntity> captor = ArgumentCaptor.forClass(CommandeEntity.class);
        verify(commandeRepository).save(captor.capture());

        LigneCommandeEntity ligne = captor.getValue().getLignes().get(0);
        assertThat(ligne.getPrixUnitaire()).isEqualByComparingTo(new BigDecimal("9.99"));
    }
}
```

### `CommandeRepositoryTest.java` (avec TestContainers)

**Chemin** : `src/test/java/com/formation/core/commandes/repository/CommandeRepositoryTest.java`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CommandeRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("formation_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",     postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommandeRepository commandeRepository;

    @Test
    void findByUtilisateurId_retourne_uniquement_les_commandes_de_cet_utilisateur() {
        UtilisateurEntity alice = em.persist(unUtilisateur("alice@test.com"));
        UtilisateurEntity bob   = em.persist(unUtilisateur("bob@test.com"));
        em.persist(uneCommande(alice));
        em.persist(uneCommande(alice));
        em.persist(uneCommande(bob));
        em.flush();

        List<CommandeEntity> commandes = commandeRepository.findByUtilisateurId(alice.getId());

        assertThat(commandes).hasSize(2);
        assertThat(commandes).allMatch(c -> c.getUtilisateur().getId().equals(alice.getId()));
    }

    @Test
    void save_cascade_les_lignes_de_commande() {
        UtilisateurEntity utilisateur = em.persist(unUtilisateur("test@test.com"));
        CategorieEntity categorie     = em.persist(uneCategorie());
        ProduitEntity produit         = em.persist(unProduit(categorie));
        em.flush();

        CommandeEntity commande = new CommandeEntity();
        commande.setUtilisateur(utilisateur);
        commande.setMontantTotal(new BigDecimal("9.99"));

        LigneCommandeEntity ligne = new LigneCommandeEntity();
        ligne.setCommande(commande);
        ligne.setProduit(produit);
        ligne.setQuantite(1);
        ligne.setPrixUnitaire(new BigDecimal("9.99"));
        commande.getLignes().add(ligne);

        CommandeEntity sauvegardee = commandeRepository.save(commande);
        em.flush();
        em.clear(); // vide le cache de 1er niveau → force un vrai SELECT

        CommandeEntity rechargee = em.find(CommandeEntity.class, sauvegardee.getId());
        assertThat(rechargee.getLignes()).hasSize(1);
    }

    // Helpers
    private UtilisateurEntity unUtilisateur(String email) { /* ... */ }
    private CommandeEntity uneCommande(UtilisateurEntity u) { /* ... */ }
}
```

> **`em.clear()` après `flush()`** : vide le cache de premier niveau de Hibernate. Sans ça, `em.find()` renvoie l'objet déjà en mémoire au lieu de faire un `SELECT` — le test ne vérifierait pas vraiment la persistance.

### Ajout de la dépendance TestContainers dans `build.gradle.kts`

```kotlin
dependencies {
    // ... dépendances existantes ...
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

---

## 🧪 Lancer les tests

### Tous les tests

```bash
./gradlew test
```

### Un fichier de test spécifique

```bash
./gradlew test --tests "com.formation.core.commandes.controller.CommandeControllerTest"
```

### Avec rapport de couverture (JaCoCo)

```bash
./gradlew test jacocoTestReport
# Rapport HTML généré dans : build/reports/jacoco/test/html/index.html
```

### Vérifier que les tests s'exécutent dans le bon ordre

```
Tests unitaires    → rapides, lancés en premier, pas besoin de Docker
@WebMvcTest        → Spring MVC seulement, pas de Docker
@DataJpaTest       → nécessite Docker (TestContainers démarre PostgreSQL)
```

> TestContainers démarre le container au premier `@DataJpaTest` et le réutilise pour tous les tests de la même JVM (Singleton pattern).

---

## 💡 Pièges courants

### `@MockBean` vs `@MockitoBean`

```java
// ❌ Déprécié depuis Spring Boot 3.4
@MockBean
private IProduitService produitService;

// ✅ Remplaçant officiel
@MockitoBean
private IProduitService produitService;
```

### Oublier `em.flush()` + `em.clear()` dans les tests `@DataJpaTest`

```java
// ❌ Le find retourne l'objet en cache mémoire, pas ce qui est en base
em.persist(commande);
CommandeEntity result = em.find(CommandeEntity.class, commande.getId());

// ✅ Force un vrai aller-retour en base
em.persist(commande);
em.flush();  // écrit en base (dans la transaction)
em.clear();  // vide le cache L1
CommandeEntity result = em.find(CommandeEntity.class, commande.getId());
```

### `@DataJpaTest` ne charge pas les `@Service` ni les `@Controller`

Si votre test `@DataJpaTest` a besoin d'un bean Spring qui n'est pas un Repository ou une Entity, utilisez `@Import` :

```java
@DataJpaTest
@Import(CommandeMapper.class) // importe un bean spécifique
class CommandeRepositoryTest { ... }
```

---

## ✅ Prochaines étapes

1. ✅ Vous savez choisir le bon niveau de test pour chaque couche
2. ✅ Vous utilisez `@WebMvcTest` pour tester les controllers sans la base de données
3. ✅ Vous utilisez `@DataJpaTest` + TestContainers pour tester les repositories sur PostgreSQL réel

Vous avez maintenant couvert toutes les couches de test de l'application.

---

*Dernière mise à jour : 2026-09-30*
