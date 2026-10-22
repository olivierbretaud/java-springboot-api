package com.formation.core.commandes.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.formation.core.commandes.dto.CommandeCreatePayload;
import com.formation.core.commandes.dto.CommandeDto;
import com.formation.core.commandes.dto.CommandeListResponse;
import com.formation.core.commandes.dto.LigneCommandePayload;
import com.formation.core.commandes.mapper.CommandeMapper;
import com.formation.engine.entities.CommandeEntity;
import com.formation.engine.entities.LigneCommandeEntity;
import com.formation.engine.entities.ProduitEntity;
import com.formation.engine.entities.StatutCommandeEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.repositories.CommandeRepository;
import com.formation.engine.repositories.ProduitRepository;
import com.formation.engine.repositories.StatutCommandeRepository;
import com.formation.engine.repositories.UtilisateurRepository;

import com.formation.engine.exceptions.CommandeNonSupprimableException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null") // @NotNull Bean Validation garantit la non-nullité en amont (@Valid sur @RequestBody/@RequestParam)
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
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé : " + utilisateurId));

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
            ligne.setPrixUnitaire(produit.getPrix());
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
