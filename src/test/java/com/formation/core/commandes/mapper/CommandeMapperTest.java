package com.formation.core.commandes.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.formation.core.commandes.dto.CommandeDto;
import com.formation.engine.entities.CommandeEntity;
import com.formation.engine.entities.LigneCommandeEntity;
import com.formation.engine.entities.ProduitEntity;
import com.formation.engine.entities.StatutCommandeEntity;

class CommandeMapperTest {

    private CommandeMapper commandeMapper;

    @BeforeEach
    void setUp() {
        commandeMapper = new CommandeMapper();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StatutCommandeEntity statut(Long id, String code, String libelle) {
        StatutCommandeEntity s = new StatutCommandeEntity();
        s.setId(id);
        s.setCode(code);
        s.setLibelle(libelle);
        return s;
    }

    private LigneCommandeEntity ligne(Long id, String nomProduit, int quantite, BigDecimal prix) {
        ProduitEntity produit = new ProduitEntity();
        produit.setNom(nomProduit);

        LigneCommandeEntity l = new LigneCommandeEntity();
        l.setId(id);
        l.setQuantite(quantite);
        l.setPrixUnitaire(prix);
        l.setProduit(produit);
        return l;
    }

    private CommandeEntity commande(Long id, StatutCommandeEntity statut, BigDecimal montant,
            LocalDateTime dateCreation, List<LigneCommandeEntity> lignes) {
        CommandeEntity c = new CommandeEntity();
        c.setId(id);
        c.setStatut(statut);
        c.setMontantTotal(montant);
        c.setDate_creation(dateCreation);
        lignes.forEach(c.getLignes()::add);
        return c;
    }

    // ── toCommandeDto ─────────────────────────────────────────────────────────

    @Test
    void toCommandeDto_retourne_null_si_entity_null() {
        assertThat(commandeMapper.toCommandeDto(null)).isNull();
    }

    @Test
    void toCommandeDto_mappe_id_montant_et_dateCreation() {
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 10, 0);
        CommandeEntity entity = commande(
                42L,
                statut(1L, "EN_ATTENTE", "En attente"),
                BigDecimal.valueOf(99.99),
                date,
                List.of());

        CommandeDto dto = commandeMapper.toCommandeDto(entity);

        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getMontantTotal()).isEqualByComparingTo("99.99");
        assertThat(dto.getDateCreation()).isEqualTo(date);
    }

    @Test
    void toCommandeDto_mappe_statut_id_et_libelle() {
        CommandeEntity entity = commande(
                1L,
                statut(3L, "EXPEDIEE", "Expédiée"),
                BigDecimal.ONE,
                null,
                List.of());

        CommandeDto dto = commandeMapper.toCommandeDto(entity);

        assertThat(dto.getStatut().getId()).isEqualTo(3L);
        assertThat(dto.getStatut().getLabel()).isEqualTo("Expédiée");
    }

    @Test
    void toCommandeDto_retourne_liste_vide_si_pas_de_lignes() {
        CommandeEntity entity = commande(
                1L, statut(1L, "EN_ATTENTE", "En attente"), BigDecimal.ONE, null, List.of());

        CommandeDto dto = commandeMapper.toCommandeDto(entity);

        assertThat(dto.getLignes()).isEmpty();
    }

    @Test
    void toCommandeDto_mappe_les_lignes() {
        CommandeEntity entity = commande(
                1L,
                statut(1L, "EN_ATTENTE", "En attente"),
                BigDecimal.valueOf(25.98),
                null,
                List.of(
                        ligne(10L, "Café arabica", 2, BigDecimal.valueOf(12.99)),
                        ligne(11L, "Thé vert", 1, BigDecimal.valueOf(9.99))));

        CommandeDto dto = commandeMapper.toCommandeDto(entity);

        assertThat(dto.getLignes()).hasSize(2);
        assertThat(dto.getLignes().get(0).getNomProduit()).isEqualTo("Café arabica");
        assertThat(dto.getLignes().get(1).getNomProduit()).isEqualTo("Thé vert");
    }

    @Test
    void toCommandeDto_mappe_quantite_et_prixUnitaire_de_la_ligne() {
        CommandeEntity entity = commande(
                1L,
                statut(1L, "EN_ATTENTE", "En attente"),
                BigDecimal.valueOf(25.98),
                null,
                List.of(ligne(10L, "Café arabica", 2, BigDecimal.valueOf(12.99))));

        CommandeDto dto = commandeMapper.toCommandeDto(entity);

        assertThat(dto.getLignes().get(0).getId()).isEqualTo(10L);
        assertThat(dto.getLignes().get(0).getQuantite()).isEqualTo(2);
        assertThat(dto.getLignes().get(0).getPrixUnitaire()).isEqualByComparingTo("12.99");
    }

    @Test
    void toCommandeDto_calcule_montantLigne() {
        CommandeEntity entity = commande(
                1L,
                statut(1L, "EN_ATTENTE", "En attente"),
                BigDecimal.valueOf(25.98),
                null,
                List.of(ligne(10L, "Café arabica", 2, BigDecimal.valueOf(12.99))));

        CommandeDto dto = commandeMapper.toCommandeDto(entity);

        // 2 × 12.99 = 25.98
        assertThat(dto.getLignes().get(0).getMontantLigne()).isEqualByComparingTo("25.98");
    }
}
