package com.formation.core.produits.dto;

import java.math.BigDecimal;

import com.formation.engine.pagination.PageParams;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProduitSearchParams extends PageParams {

    private String search;
    private Long categorieId;
    private BigDecimal prixMin;
    private BigDecimal prixMax;

    public ProduitSearchParams() {
        setSort("nom");
    }
}
