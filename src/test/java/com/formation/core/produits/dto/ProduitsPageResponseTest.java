package com.formation.core.produits.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ProduitsPageResponseTest {

    @Test
    void constructor_mappe_les_champs_depuis_page() {
        ProduitDto dto1 = new ProduitDto();
        dto1.setId(1L);
        dto1.setNom("Pomme");

        ProduitDto dto2 = new ProduitDto();
        dto2.setId(2L);
        dto2.setNom("Poire");

        PageRequest pageable = PageRequest.of(0, 10);
        Page<ProduitDto> page = new PageImpl<>(List.of(dto1, dto2), pageable, 2);

        ProduitsPageResponse response = new ProduitsPageResponse(page);

        assertThat(response.getProduits()).hasSize(2);
        assertThat(response.getProduits().get(0).getNom()).isEqualTo("Pomme");
        assertThat(response.getProduits().get(1).getNom()).isEqualTo("Poire");
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isDernierePage()).isTrue();
    }

    @Test
    void constructor_derniere_page_est_false_quand_plusieurs_pages() {
        ProduitDto dto = new ProduitDto();
        dto.setId(1L);

        PageRequest pageable = PageRequest.of(0, 1);
        Page<ProduitDto> page = new PageImpl<>(List.of(dto), pageable, 5);

        ProduitsPageResponse response = new ProduitsPageResponse(page);

        assertThat(response.isDernierePage()).isFalse();
        assertThat(response.getTotalPages()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getSize()).isEqualTo(1);
    }

    @Test
    void constructor_page_vide_retourne_valeurs_zero() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ProduitDto> page = new PageImpl<>(List.of(), pageable, 0);

        ProduitsPageResponse response = new ProduitsPageResponse(page);

        assertThat(response.getProduits()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
        assertThat(response.isDernierePage()).isTrue();
    }

    @Test
    void constructor_mappe_le_numero_de_page_courant() {
        ProduitDto dto = new ProduitDto();
        dto.setId(1L);

        PageRequest pageable = PageRequest.of(2, 5);
        Page<ProduitDto> page = new PageImpl<>(List.of(dto), pageable, 15);

        ProduitsPageResponse response = new ProduitsPageResponse(page);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isDernierePage()).isTrue();
    }
}
