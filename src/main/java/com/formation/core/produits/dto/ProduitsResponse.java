package com.formation.core.produits.dto;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProduitsResponse {

    private final List<ProduitDto> produits;

}
