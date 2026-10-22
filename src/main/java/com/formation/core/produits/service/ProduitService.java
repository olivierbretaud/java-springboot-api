package com.formation.core.produits.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.formation.core.produits.dto.ProduitCreatePayload;
import com.formation.core.produits.dto.ProduitDto;
import com.formation.core.produits.dto.ProduitSearchParams;
import com.formation.core.produits.mapper.ProduitMapper;
import com.formation.core.produits.specification.ProduitSpecification;
import com.formation.engine.pagination.PageResponse;
import com.formation.engine.entities.CategorieEntity;
import com.formation.engine.entities.ProduitEntity;
import com.formation.engine.repositories.CategorieRepository;
import jakarta.persistence.EntityNotFoundException;
import com.formation.engine.repositories.ProduitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProduitService implements IProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final ProduitMapper produitMapper;

    @Override
    public List<ProduitDto> listProduits() {
        return produitMapper.toProduitDtoList(produitRepository.findAll());
    }

    @Override
    public PageResponse<ProduitDto> searchProduits(ProduitSearchParams params) {
        return new PageResponse<>(
            produitRepository.findAll(ProduitSpecification.from(params), params.toPageable())
                .map(produitMapper::toProduitDto)
        );
    }

    @Override
    public ProduitDto getProduitById(Long id) {
        return produitMapper.toProduitDto(produitRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable : " + id)));
    }

    @Override
    public ProduitDto createProduit(ProduitCreatePayload request) {
        CategorieEntity categorie = categorieRepository.findById(Objects.requireNonNull(request.getIdCategorie()))
                .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable : " + request.getIdCategorie()));

        return produitMapper.toProduitDto(produitRepository.save(Objects.requireNonNull(produitMapper.toEntity(request, categorie))));
    }

    @Override
    public ProduitDto updateProduit(Long id, ProduitCreatePayload request) {
        ProduitEntity produit = produitRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable : " + id));

        CategorieEntity categorie = categorieRepository.findById(Objects.requireNonNull(request.getIdCategorie()))
                .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable : " + request.getIdCategorie()));

        produit.setNom(request.getNom());
        produit.setDescription(request.getDescription());
        produit.setPrix(request.getPrix());
        produit.setUniteParPackaging(request.getUniteParPackaging());
        produit.setCategorie(categorie);

        return produitMapper.toProduitDto(produitRepository.save(produit));
    }

    @Override
    public void deleteProduit(Long id) {
        Long nonNullId = Objects.requireNonNull(id);
        if (!produitRepository.existsById(nonNullId)) {
            throw new EntityNotFoundException("Produit introuvable : " + nonNullId);
        }
        produitRepository.deleteById(nonNullId);
    }
}
