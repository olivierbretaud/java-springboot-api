package com.formation.core.produits.mapper;

import com.formation.core.produits.dto.ProduitDto;
import com.formation.core.produits.dto.ProduitCreatePayload;
import com.formation.engine.entities.CategorieEntity;
import com.formation.engine.entities.ProduitEntity;
import java.math.BigDecimal;
import com.formation.engine.utils.EntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProduitMapperTest {

    private ProduitMapper produitMapper;

    @BeforeEach
    void setUp() {
        produitMapper = new ProduitMapper(new EntityMapper());
    }

    @Test
    void toProduitDto_mappe_correctement_entity_vers_dto() {
        ProduitEntity entity = new ProduitEntity();
        entity.setId(1L);
        entity.setNom("Banane");

        ProduitDto dto = produitMapper.toProduitDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNom()).isEqualTo("Banane");
    }

    @Test
    void toProduitDto_retourne_null_si_entity_null() {
        ProduitDto dto = produitMapper.toProduitDto(null);

        assertThat(dto).isNull();
    }

    @Test
    void toProduitDtoList_mappe_liste_entities() {
        ProduitEntity e1 = new ProduitEntity();
        e1.setId(1L);
        e1.setNom("Pomme");

        ProduitEntity e2 = new ProduitEntity();
        e2.setId(2L);
        e2.setNom("Poire");

        List<ProduitDto> dtos = produitMapper.toProduitDtoList(List.of(e1, e2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getNom()).isEqualTo("Pomme");
        assertThat(dtos.get(1).getNom()).isEqualTo("Poire");
    }

    @Test
    void toProduitDtoList_retourne_liste_vide_si_liste_nulle() {
        List<ProduitDto> dtos = produitMapper.toProduitDtoList(null);

        assertThat(dtos).isEmpty();
    }

    @Test
    void toEntity_mappe_correctement_request_vers_entity() {
        ProduitCreatePayload request = new ProduitCreatePayload();
        request.setNom("Banane");
        request.setDescription("Fruit jaune");
        request.setPrix(new BigDecimal("1.50"));
        request.setUniteParPackaging(6);

        CategorieEntity categorie = new CategorieEntity();
        categorie.setId(10L);
        categorie.setNom("Fruits");

        ProduitEntity entity = produitMapper.toEntity(request, categorie);

        assertThat(entity.getNom()).isEqualTo("Banane");
        assertThat(entity.getDescription()).isEqualTo("Fruit jaune");
        assertThat(entity.getPrix()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(entity.getUniteParPackaging()).isEqualTo(6);
        assertThat(entity.getCategorie()).isEqualTo(categorie);
    }

    @Test
    void toEntity_mappe_correctement_sans_categorie() {
        ProduitCreatePayload request = new ProduitCreatePayload();
        request.setNom("Pomme");
        request.setDescription("Fruit rouge");
        request.setPrix(new BigDecimal("2.00"));
        request.setUniteParPackaging(4);

        ProduitEntity entity = produitMapper.toEntity(request, null);

        assertThat(entity.getNom()).isEqualTo("Pomme");
        assertThat(entity.getCategorie()).isNull();
    }
}
