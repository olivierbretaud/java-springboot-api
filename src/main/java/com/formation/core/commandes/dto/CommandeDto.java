package com.formation.core.commandes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.formation.core.referentiel.dto.StatutCommandeDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommandeDto {

    private Long id;
    private StatutCommandeDto statut;
    private BigDecimal montantTotal;
    private LocalDateTime dateCreation;
    private List<LigneCommandeDto> lignes;
}
