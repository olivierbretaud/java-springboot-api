package com.formation.core.commandes.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LigneCommandeDto {

    private Long id;
    private String nomProduit;
    private Integer quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal montantLigne;
}
