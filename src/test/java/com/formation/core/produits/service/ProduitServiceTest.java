package com.formation.core.produits.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.formation.core.produits.dto.ProduitCreatePayload;
import com.formation.core.produits.dto.ProduitDto;
import com.formation.core.produits.dto.ProduitSearchParams;
import com.formation.engine.pagination.PageResponse;
import com.formation.core.produits.mapper.ProduitMapper;
import com.formation.engine.entities.CategorieEntity;
import com.formation.engine.entities.ProduitEntity;
import com.formation.engine.repositories.CategorieRepository;
import com.formation.engine.repositories.ProduitRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private CategorieRepository categorieRepository;

    @Mock
    private ProduitMapper produitMapper;

    @InjectMocks
    private ProduitService produitService;

    @Test
    @SuppressWarnings({"unchecked", "null"})
    void searchProduits_retourne_page_mappee() {
        ProduitEntity entity = new ProduitEntity();
        entity.setId(1L);
        entity.setNom("Pomme");

        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Pomme");

        when(produitRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(produitMapper.toProduitDto(entity)).thenReturn(dto);

        ProduitSearchParams params = new ProduitSearchParams();
        PageResponse<ProduitDto> result = produitService.searchProduits(params);

        assertThat(result.getContenu()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContenu().get(0).getNom()).isEqualTo("Pomme");
    }

    @Test
    @SuppressWarnings({"unchecked", "null"})
    void searchProduits_retourne_page_vide_si_aucun_resultat() {
        when(produitRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ProduitSearchParams params = new ProduitSearchParams();
        PageResponse<ProduitDto> result = produitService.searchProduits(params);

        assertThat(result.getContenu()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void listProduits_retourne_liste_mappee() {
        ProduitEntity entity = new ProduitEntity();
        entity.setId(1L);
        entity.setNom("Pomme");

        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Pomme");

        when(produitRepository.findAll()).thenReturn(List.of(entity));
        when(produitMapper.toProduitDtoList(List.of(entity))).thenReturn(List.of(dto));

        List<ProduitDto> result = produitService.listProduits();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getNom()).isEqualTo("Pomme");
    }

    @Test
    void listProduits_retourne_liste_vide_si_repository_vide() {
        when(produitRepository.findAll()).thenReturn(List.of());
        when(produitMapper.toProduitDtoList(List.of())).thenReturn(List.of());

        List<ProduitDto> result = produitService.listProduits();

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("null")
    void createProduit_retourne_dto_cree() {
        ProduitCreatePayload request = new ProduitCreatePayload();
        request.setNom("Café arabica");
        request.setIdCategorie(1L);

        CategorieEntity categorie = new CategorieEntity();
        ProduitEntity entity = new ProduitEntity();
        ProduitEntity sauvegarde = new ProduitEntity();
        sauvegarde.setId(1L);

        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Café arabica");

        when(categorieRepository.findById(1L)).thenReturn(Optional.of(categorie));
        when(produitMapper.toEntity(request, categorie)).thenReturn(entity);
        when(produitRepository.save(any())).thenReturn(sauvegarde);
        when(produitMapper.toProduitDto(sauvegarde)).thenReturn(dto);

        ProduitDto result = produitService.createProduit(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNom()).isEqualTo("Café arabica");
        verify(produitMapper).toEntity(request, categorie);
    }

    
    @Test
    void createProduit_leve_exception_si_categorie_inexistante() {
        ProduitCreatePayload request = new ProduitCreatePayload();
        request.setIdCategorie(99L);

        when(categorieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> produitService.createProduit(request));
    }

    @Test
    void getProduitById_retourne_dto_si_trouve() {
        ProduitEntity entity = new ProduitEntity();
        entity.setId(1L);
        entity.setNom("Pomme");

        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Pomme");

        when(produitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(produitMapper.toProduitDto(entity)).thenReturn(dto);

        ProduitDto result = produitService.getProduitById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNom()).isEqualTo("Pomme");
    }

    @Test
    void getProduitById_leve_exception_si_produit_inexistant() {
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> produitService.getProduitById(99L));
    }

    @Test
    void updateProduit_retourne_dto_mis_a_jour() {
        ProduitCreatePayload request = new ProduitCreatePayload();
        request.setNom("Café robusta");
        request.setDescription("Nouvelle description");
        request.setPrix(BigDecimal.valueOf(12.50));
        request.setUniteParPackaging(6);
        request.setIdCategorie(2L);

        ProduitEntity existant = new ProduitEntity();
        existant.setId(1L);
        existant.setNom("Café arabica");

        CategorieEntity categorie = new CategorieEntity();
        ProduitEntity sauvegarde = new ProduitEntity();
        sauvegarde.setId(1L);
        sauvegarde.setNom("Café robusta");

        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Café robusta");

        when(produitRepository.findById(1L)).thenReturn(Optional.of(existant));
        when(categorieRepository.findById(2L)).thenReturn(Optional.of(categorie));
        when(produitRepository.save(existant)).thenReturn(sauvegarde);
        when(produitMapper.toProduitDto(sauvegarde)).thenReturn(dto);

        ProduitDto result = produitService.updateProduit(1L, request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNom()).isEqualTo("Café robusta");
        verify(produitRepository).save(existant);
    }

    @Test
    void updateProduit_leve_exception_si_produit_inexistant() {
        ProduitCreatePayload request = new ProduitCreatePayload();
        request.setIdCategorie(1L);

        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> produitService.updateProduit(99L, request));
    }

    @Test
    void updateProduit_leve_exception_si_categorie_inexistante() {
        ProduitCreatePayload request = new ProduitCreatePayload();
        request.setIdCategorie(99L);

        ProduitEntity existant = new ProduitEntity();
        existant.setId(1L);

        when(produitRepository.findById(1L)).thenReturn(Optional.of(existant));
        when(categorieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> produitService.updateProduit(1L, request));
    }

    @Test
    void deleteProduit_supprime_le_produit() {
        when(produitRepository.existsById(1L)).thenReturn(true);

        produitService.deleteProduit(1L);

        verify(produitRepository).deleteById(1L);
    }

    @Test
    void deleteProduit_leve_exception_si_produit_inexistant() {
        when(produitRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> produitService.deleteProduit(99L));
    }
}
