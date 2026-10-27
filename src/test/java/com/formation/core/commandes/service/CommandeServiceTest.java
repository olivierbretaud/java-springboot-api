package com.formation.core.commandes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.formation.core.commandes.dto.CommandeCreatePayload;
import com.formation.core.commandes.dto.CommandeDto;
import com.formation.core.commandes.dto.CommandeListResponse;
import com.formation.core.commandes.dto.LigneCommandePayload;
import com.formation.core.commandes.mapper.CommandeMapper;
import com.formation.engine.entities.CommandeEntity;
import com.formation.engine.entities.ProduitEntity;
import com.formation.engine.entities.StatutCommandeEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.exceptions.CommandeNonSupprimableException;
import com.formation.engine.repositories.CommandeRepository;
import com.formation.engine.repositories.ProduitRepository;
import com.formation.engine.repositories.StatutCommandeRepository;
import com.formation.engine.repositories.UtilisateurRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CommandeServiceTest {

    @Mock private CommandeRepository commandeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ProduitRepository produitRepository;
    @Mock private StatutCommandeRepository statutCommandeRepository;
    @Mock private CommandeMapper commandeMapper;

    @InjectMocks
    private CommandeService commandeService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StatutCommandeEntity statutEntity(Long id, String code, String libelle) {
        StatutCommandeEntity s = new StatutCommandeEntity();
        s.setId(id);
        s.setCode(code);
        s.setLibelle(libelle);
        return s;
    }

    private ProduitEntity produitEntity(Long id, BigDecimal prix) {
        ProduitEntity p = new ProduitEntity();
        p.setId(id);
        p.setNom("Produit " + id);
        p.setPrix(prix);
        return p;
    }

    private CommandeEntity commandeEntity(Long id, String codeStatut) {
        CommandeEntity c = new CommandeEntity();
        c.setId(id);
        c.setStatut(statutEntity(1L, codeStatut, codeStatut));
        c.setMontantTotal(BigDecimal.TEN);
        return c;
    }

    private CommandeDto commandeDto(Long id) {
        CommandeDto dto = new CommandeDto();
        dto.setId(id);
        return dto;
    }

    // ── creerCommande ─────────────────────────────────────────────────────────

    @Test
    void creerCommande_retourne_dto_avec_statut_en_attente() {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        ProduitEntity produit = produitEntity(1L, BigDecimal.valueOf(12.99));
        StatutCommandeEntity enAttente = statutEntity(1L, "EN_ATTENTE", "En attente");
        CommandeEntity saved = commandeEntity(1L, "EN_ATTENTE");

        LigneCommandePayload ligne = new LigneCommandePayload();
        ligne.setIdProduit(1L);
        ligne.setQuantite(2);

        CommandeCreatePayload payload = new CommandeCreatePayload();
        payload.setLignes(List.of(ligne));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(statutCommandeRepository.findByCode("EN_ATTENTE")).thenReturn(Optional.of(enAttente));
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(commandeRepository.save(any())).thenReturn(saved);
        when(commandeMapper.toCommandeDto(saved)).thenReturn(commandeDto(1L));

        CommandeDto result = commandeService.creerCommande(payload, 1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void creerCommande_calcule_montant_total() {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        ProduitEntity produit = produitEntity(1L, BigDecimal.valueOf(10.00));
        StatutCommandeEntity enAttente = statutEntity(1L, "EN_ATTENTE", "En attente");

        LigneCommandePayload ligne = new LigneCommandePayload();
        ligne.setIdProduit(1L);
        ligne.setQuantite(3);

        CommandeCreatePayload payload = new CommandeCreatePayload();
        payload.setLignes(List.of(ligne));

        ArgumentCaptor<CommandeEntity> captor = ArgumentCaptor.forClass(CommandeEntity.class);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(statutCommandeRepository.findByCode("EN_ATTENTE")).thenReturn(Optional.of(enAttente));
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(commandeRepository.save(captor.capture())).thenReturn(commandeEntity(1L, "EN_ATTENTE"));
        when(commandeMapper.toCommandeDto(any())).thenReturn(commandeDto(1L));

        commandeService.creerCommande(payload, 1L);

        assertThat(captor.getValue().getMontantTotal()).isEqualByComparingTo("30.00");
    }

    @Test
    void creerCommande_leve_exception_si_utilisateur_inexistant() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        CommandeCreatePayload payload = new CommandeCreatePayload();
        payload.setLignes(List.of());

        assertThrows(EntityNotFoundException.class,
                () -> commandeService.creerCommande(payload, 99L));
    }

    @Test
    void creerCommande_leve_exception_si_produit_inexistant() {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        StatutCommandeEntity enAttente = statutEntity(1L, "EN_ATTENTE", "En attente");

        LigneCommandePayload ligne = new LigneCommandePayload();
        ligne.setIdProduit(99L);
        ligne.setQuantite(1);

        CommandeCreatePayload payload = new CommandeCreatePayload();
        payload.setLignes(List.of(ligne));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(statutCommandeRepository.findByCode("EN_ATTENTE")).thenReturn(Optional.of(enAttente));
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commandeService.creerCommande(payload, 1L));
    }

    // ── getCommandeById ───────────────────────────────────────────────────────

    @Test
    void getCommandeById_retourne_dto() {
        CommandeEntity entity = commandeEntity(1L, "EN_ATTENTE");
        when(commandeRepository.findByIdWithLignesAndProduits(1L)).thenReturn(Optional.of(entity));
        when(commandeMapper.toCommandeDto(entity)).thenReturn(commandeDto(1L));

        CommandeDto result = commandeService.getCommandeById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getCommandeById_leve_exception_si_inexistant() {
        when(commandeRepository.findByIdWithLignesAndProduits(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commandeService.getCommandeById(99L));
    }

    // ── listCommandesByUtilisateur ────────────────────────────────────────────

    @Test
    void listCommandesByUtilisateur_retourne_wrapper_avec_commandes() {
        CommandeEntity entity = commandeEntity(1L, "EN_ATTENTE");
        when(commandeRepository.findByUtilisateurId(1L)).thenReturn(List.of(entity));
        when(commandeMapper.toCommandeDto(entity)).thenReturn(commandeDto(1L));

        CommandeListResponse result = commandeService.listCommandesByUtilisateur(1L);

        assertThat(result.getCommandes()).hasSize(1);
        assertThat(result.getCommandes().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void listCommandesByUtilisateur_retourne_liste_vide() {
        when(commandeRepository.findByUtilisateurId(1L)).thenReturn(List.of());

        CommandeListResponse result = commandeService.listCommandesByUtilisateur(1L);

        assertThat(result.getCommandes()).isEmpty();
    }

    // ── changerStatut ─────────────────────────────────────────────────────────

    @Test
    void changerStatut_met_a_jour_le_statut() {
        CommandeEntity commande = commandeEntity(1L, "EN_ATTENTE");
        StatutCommandeEntity confirmee = statutEntity(2L, "CONFIRMEE", "Confirmée");

        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(statutCommandeRepository.findById(2L)).thenReturn(Optional.of(confirmee));
        when(commandeRepository.save(commande)).thenReturn(commande);
        when(commandeMapper.toCommandeDto(commande)).thenReturn(commandeDto(1L));

        commandeService.changerStatut(1L, 2L);

        assertThat(commande.getStatut().getCode()).isEqualTo("CONFIRMEE");
        verify(commandeRepository).save(commande);
    }

    @Test
    void changerStatut_leve_exception_si_commande_inexistante() {
        when(commandeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commandeService.changerStatut(99L, 2L));
    }

    @Test
    void changerStatut_leve_exception_si_statut_inexistant() {
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commandeEntity(1L, "EN_ATTENTE")));
        when(statutCommandeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commandeService.changerStatut(1L, 99L));
    }

    // ── deleteCommande ────────────────────────────────────────────────────────

    @Test
    void deleteCommande_supprime_commande_en_attente() {
        CommandeEntity commande = commandeEntity(1L, "EN_ATTENTE");
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));

        commandeService.deleteCommande(1L);

        verify(commandeRepository).delete(commande);
    }

    @Test
    void deleteCommande_leve_exception_si_statut_non_en_attente() {
        CommandeEntity commande = commandeEntity(1L, "CONFIRMEE");
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));

        assertThrows(CommandeNonSupprimableException.class,
                () -> commandeService.deleteCommande(1L));
    }

    @Test
    void deleteCommande_leve_exception_si_commande_inexistante() {
        when(commandeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commandeService.deleteCommande(99L));
    }
}
