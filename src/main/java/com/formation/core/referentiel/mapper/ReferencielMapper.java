package com.formation.core.referentiel.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.formation.core.referentiel.dto.CategorieDto;
import com.formation.core.referentiel.dto.RoleDto;
import com.formation.core.referentiel.dto.StatutCommandeDto;
import com.formation.engine.entities.CategorieEntity;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.StatutCommandeEntity;
import com.formation.engine.utils.EntityMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReferencielMapper {

    private final EntityMapper entityMapper;

    public CategorieDto toCategoryDto(CategorieEntity entity) {
        return entityMapper.toDto(entity, e -> {
            CategorieDto dto = new CategorieDto();
            dto.setId(e.getId());
            dto.setLabel(e.getNom());
            return dto;
        });
    }

    public List<CategorieDto> toCategoryDtoList(List<CategorieEntity> entities) {
        return entityMapper.toDtoList(entities, this::toCategoryDto);
    }

    public StatutCommandeDto toStatutCommandeDto(StatutCommandeEntity entity) {
        return entityMapper.toDto(entity, e -> {
            StatutCommandeDto dto = new StatutCommandeDto();
            dto.setId(e.getId());
            dto.setLabel(e.getLibelle());
            return dto;
        });
    }

    public List<StatutCommandeDto> toStatutCommandeDtoList(List<StatutCommandeEntity> entities) {
        return entityMapper.toDtoList(entities, this::toStatutCommandeDto);
    }

    public RoleDto toRoleDto(RoleEntity entity) {
        return entityMapper.toDto(entity, e -> {
            RoleDto dto = new RoleDto();
            dto.setId(e.getId());
            dto.setLabel(e.getLibelle());
            return dto;
        });
    }

    public List<RoleDto> toRoleDtoList(List<RoleEntity> entities) {
        return entityMapper.toDtoList(entities, this::toRoleDto);
    }
}
