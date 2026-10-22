package com.formation.core.produits.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import com.formation.core.produits.dto.ProduitCreatePayload;
import com.formation.core.produits.dto.ProduitDto;
import com.formation.core.produits.dto.ProduitSearchParams;
import com.formation.core.produits.dto.ProduitsResponse;
import com.formation.core.produits.service.IProduitService;
import com.formation.engine.pagination.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/produit")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class ProduitController {

    private final IProduitService produitService;

    @GetMapping
    public ProduitsResponse getProduits() {
        return new ProduitsResponse(produitService.listProduits());
    }

    @GetMapping("/search")
    public PageResponse<ProduitDto> searchProduits(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) BigDecimal prixMin,
            @RequestParam(required = false) BigDecimal prixMax,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "nom") @Pattern(regexp = "nom|prix|date_creation|date_modification", message = "Le tri est autorisé sur : nom, prix, date_creation, date_modification") String sort,
            @RequestParam(defaultValue = "ASC") @Pattern(regexp = "(?i)ASC|DESC", message = "La direction doit être ASC ou DESC") String direction
        ) {

        ProduitSearchParams params = new ProduitSearchParams();
        params.setSearch(search);
        params.setCategorieId(categorieId);
        params.setPrixMin(prixMin);
        params.setPrixMax(prixMax);
        params.setPage(page);
        params.setSize(size);
        params.setSort(sort);
        params.setDirection(direction);

        return produitService.searchProduits(params);
    }

    @GetMapping("/{id}")
    public ProduitDto getProduitById(@PathVariable Long id) {
        return produitService.getProduitById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProduitDto createProduit(@Valid @RequestBody ProduitCreatePayload request) {
        return produitService.createProduit(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProduitDto updateProduit(@PathVariable Long id, @Valid @RequestBody ProduitCreatePayload request) {
        return produitService.updateProduit(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduit(@PathVariable Long id) {
        produitService.deleteProduit(id);
    }

}
