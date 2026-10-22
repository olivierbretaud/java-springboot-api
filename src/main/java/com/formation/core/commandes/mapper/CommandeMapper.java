package com.formation.core.commandes.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.formation.core.commandes.dto.CommandeDto;
import com.formation.core.commandes.dto.LigneCommandeDto;
import com.formation.core.referentiel.dto.StatutCommandeDto;
import com.formation.engine.entities.CommandeEntity;
import com.formation.engine.entities.LigneCommandeEntity;
import com.formation.engine.entities.StatutCommandeEntity;

@Component
public class CommandeMapper {

    public CommandeDto toCommandeDto(CommandeEntity entity) {
        if (entity == null) return null;

        CommandeDto dto = new CommandeDto();
        dto.setId(entity.getId());
        dto.setStatut(toStatutDto(entity.getStatut()));
        dto.setMontantTotal(entity.getMontantTotal());
        dto.setDateCreation(entity.getDate_creation());
        dto.setLignes(entity.getLignes().stream()
                .map(this::toLigneCommandeDto)
                .toList());
        return dto;
    }

    private StatutCommandeDto toStatutDto(StatutCommandeEntity statut) {
        StatutCommandeDto dto = new StatutCommandeDto();
        dto.setId(statut.getId());
        dto.setLabel(statut.getLibelle());
        return dto;
    }

    private LigneCommandeDto toLigneCommandeDto(LigneCommandeEntity entity) {
        LigneCommandeDto dto = new LigneCommandeDto();
        dto.setId(entity.getId());
        dto.setNomProduit(entity.getProduit().getNom());
        dto.setQuantite(entity.getQuantite());
        dto.setPrixUnitaire(entity.getPrixUnitaire());
        dto.setMontantLigne(
                entity.getPrixUnitaire().multiply(BigDecimal.valueOf(entity.getQuantite())));
        return dto;
    }
}
