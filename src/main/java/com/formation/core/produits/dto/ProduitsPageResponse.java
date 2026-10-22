package com.formation.core.produits.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

@Getter
public class ProduitsPageResponse {

    private final List<ProduitDto> produits;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean dernierePage;

    public ProduitsPageResponse(Page<ProduitDto> page) {
        this.produits = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.dernierePage = page.isLast();
    }
}
