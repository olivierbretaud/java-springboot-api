package com.formation.engine.repositories.commande;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.formation.config.JpaAuditingTestConfig;
import com.formation.engine.entities.CategorieEntity;
import com.formation.engine.entities.CommandeEntity;
import com.formation.engine.entities.LigneCommandeEntity;
import com.formation.engine.entities.ProduitEntity;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.StatutCommandeEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.repositories.CommandeRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(JpaAuditingTestConfig.class)
class CommandeRepositoryTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("formation_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",          postgres::getJdbcUrl);
        registry.add("spring.datasource.username",     postgres::getUsername);
        registry.add("spring.datasource.password",     postgres::getPassword);
        registry.add("spring.liquibase.change-log",
                     () -> "classpath:db/changelog/db.changelog-master.yaml");
    }

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommandeRepository commandeRepository;

    @Test
    void findByUtilisateurId_retourne_uniquement_les_commandes_de_cet_utilisateur() {
        RoleEntity role            = em.persist(unRole());
        UtilisateurEntity alice    = em.persist(unUtilisateur("alice@test.com", role));
        UtilisateurEntity bob      = em.persist(unUtilisateur("bob@test.com",   role));
        StatutCommandeEntity statut = em.persist(unStatut());
        em.persist(uneCommande(alice, statut));
        em.persist(uneCommande(alice, statut));
        em.persist(uneCommande(bob,   statut));
        em.flush();

        List<CommandeEntity> commandes = commandeRepository.findByUtilisateurId(alice.getId());

        assertThat(commandes).hasSize(2)
            .allMatch(c -> c.getUtilisateur().getId().equals(alice.getId()));
    }

    @Test
    void findByUtilisateurId_retourne_liste_vide_si_aucune_commande() {
        RoleEntity role = em.persist(unRole());
        UtilisateurEntity utilisateur = em.persist(unUtilisateur("solo@test.com", role));
        em.flush();

        List<CommandeEntity> commandes = commandeRepository.findByUtilisateurId(utilisateur.getId());

        assertThat(commandes).isEmpty();
    }

    @Test
    void findByIdWithLignesAndProduits_charge_les_lignes_et_produits() {
        RoleEntity role             = em.persist(unRole());
        UtilisateurEntity u         = em.persist(unUtilisateur("fetch@test.com", role));
        StatutCommandeEntity statut = em.persist(unStatut());
        CategorieEntity cat         = em.persist(uneCategorie());
        ProduitEntity p1            = em.persist(unProduit("Pomme",  new BigDecimal("1.00"), cat));
        ProduitEntity p2            = em.persist(unProduit("Banane", new BigDecimal("2.00"), cat));

        CommandeEntity commande = uneCommande(u, statut);
        commande.getLignes().add(uneLigne(commande, p1, 1, new BigDecimal("1.00")));
        commande.getLignes().add(uneLigne(commande, p2, 2, new BigDecimal("2.00")));
        em.persist(commande);
        em.flush();
        em.clear();

        Optional<CommandeEntity> result = commandeRepository.findByIdWithLignesAndProduits(commande.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getLignes()).hasSize(2);
        assertThat(result.get().getLignes())
                .extracting(l -> l.getProduit().getNom())
                .containsExactlyInAnyOrder("Pomme", "Banane");
    }

    @Test
    void findByIdWithLignesAndProduits_retourne_vide_si_inexistant() {
        Optional<CommandeEntity> result = commandeRepository.findByIdWithLignesAndProduits(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_cascade_les_lignes_de_commande() {
        RoleEntity role             = em.persist(unRole());
        UtilisateurEntity u         = em.persist(unUtilisateur("cascade@test.com", role));
        StatutCommandeEntity statut = em.persist(unStatut());
        CategorieEntity cat         = em.persist(uneCategorie());
        ProduitEntity p             = em.persist(unProduit("Kiwi", new BigDecimal("9.99"), cat));
        em.flush();

        CommandeEntity commande = uneCommande(u, statut);
        commande.getLignes().add(uneLigne(commande, p, 3, new BigDecimal("9.99")));

        CommandeEntity saved = commandeRepository.save(commande);
        em.flush();
        em.clear();

        CommandeEntity reloaded = em.find(CommandeEntity.class, saved.getId());
        assertThat(reloaded.getLignes()).hasSize(1);
        assertThat(reloaded.getLignes().get(0).getQuantite()).isEqualTo(3);
        assertThat(reloaded.getLignes().get(0).getPrixUnitaire())
                .isEqualByComparingTo(new BigDecimal("9.99"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private RoleEntity unRole() {
        RoleEntity r = new RoleEntity();
        r.setCode("ROLE_" + uuid());
        r.setLibelle("Test");
        return r;
    }

    private UtilisateurEntity unUtilisateur(String email, RoleEntity role) {
        UtilisateurEntity u = new UtilisateurEntity();
        u.setEmail(email);
        u.setMotDePasse("password");
        u.setNom("Nom");
        u.setPrenom("Prenom");
        u.setActif(true);
        u.setRole(role);
        return u;
    }

    private StatutCommandeEntity unStatut() {
        StatutCommandeEntity s = new StatutCommandeEntity();
        s.setCode("STATUT_" + uuid());
        s.setLibelle("En attente");
        return s;
    }

    private CategorieEntity uneCategorie() {
        CategorieEntity c = new CategorieEntity();
        c.setNom("Cat " + uuid());
        return c;
    }

    private ProduitEntity unProduit(String nom, BigDecimal prix, CategorieEntity categorie) {
        ProduitEntity p = new ProduitEntity();
        p.setNom(nom);
        p.setPrix(prix);
        p.setCategorie(categorie);
        return p;
    }

    private CommandeEntity uneCommande(UtilisateurEntity utilisateur, StatutCommandeEntity statut) {
        CommandeEntity c = new CommandeEntity();
        c.setUtilisateur(utilisateur);
        c.setStatut(statut);
        c.setMontantTotal(BigDecimal.ZERO);
        return c;
    }

    private LigneCommandeEntity uneLigne(CommandeEntity commande, ProduitEntity produit,
                                          int quantite, BigDecimal prixUnitaire) {
        LigneCommandeEntity l = new LigneCommandeEntity();
        l.setCommande(commande);
        l.setProduit(produit);
        l.setQuantite(quantite);
        l.setPrixUnitaire(prixUnitaire);
        return l;
    }

    private String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
