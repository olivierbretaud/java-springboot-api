package com.formation.core.produits.service;

import java.util.List;

import com.formation.core.produits.dto.ProduitCreatePayload;
import com.formation.core.produits.dto.ProduitDto;
import com.formation.core.produits.dto.ProduitSearchParams;
import com.formation.engine.pagination.PageResponse;

public interface IProduitService {
    List<ProduitDto> listProduits();
    PageResponse<ProduitDto> searchProduits(ProduitSearchParams params);
    ProduitDto getProduitById(Long id);
    ProduitDto createProduit(ProduitCreatePayload request);
    ProduitDto updateProduit(Long id, ProduitCreatePayload request);
    void deleteProduit(Long id);
}