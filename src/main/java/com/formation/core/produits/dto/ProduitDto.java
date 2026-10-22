package com.formation.core.produits.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProduitDto {

    private Long id;

    private String nom;

    private String description;

    private BigDecimal prix;

    private Integer uniteParPackaging;

    private Long categorieId;

    private String categorieName;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

}
