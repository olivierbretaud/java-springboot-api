package com.formation.core.produits.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.formation.core.produits.dto.ProduitDto;
import com.formation.core.produits.dto.ProduitCreatePayload;
import com.formation.engine.entities.CategorieEntity;
import com.formation.engine.entities.ProduitEntity;
import com.formation.engine.utils.EntityMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProduitMapper {

    private final EntityMapper entityMapper;

    public ProduitDto toProduitDto(ProduitEntity entity) {
        return entityMapper.toDto(entity, e -> {
            ProduitDto dto = new ProduitDto();
            dto.setId(e.getId());
            dto.setNom(e.getNom());
            dto.setDescription(e.getDescription());
            dto.setPrix(e.getPrix());
            dto.setUniteParPackaging(e.getUniteParPackaging());
            if (e.getCategorie() != null) {
                dto.setCategorieId(e.getCategorie().getId());
                dto.setCategorieName(e.getCategorie().getNom());
            }
            dto.setDateCreation(e.getDate_creation());
            dto.setDateModification(e.getDate_modification());
            return dto;
        });
    }

    public List<ProduitDto> toProduitDtoList(List<ProduitEntity> entities) {
        return entityMapper.toDtoList(entities, this::toProduitDto);
    }

    public ProduitEntity toEntity(ProduitCreatePayload request, CategorieEntity categorie) {
        ProduitEntity entity = new ProduitEntity();
        entity.setNom(request.getNom());
        entity.setDescription(request.getDescription());
        entity.setPrix(request.getPrix());
        entity.setUniteParPackaging(request.getUniteParPackaging());
        entity.setCategorie(categorie);
        return entity;
    }
}
