package com.formation.core.utilisateurs.mapper;

import org.springframework.stereotype.Component;

import com.formation.core.referentiel.dto.RoleDto;
import com.formation.core.utilisateurs.dto.UtilisateurCreatePayload;
import com.formation.core.utilisateurs.dto.UtilisateurDto;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.utils.EntityMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UtilisateurMapper {

    private final EntityMapper entityMapper;

    public UtilisateurDto toUtilisateurDto(UtilisateurEntity entity) {
        return entityMapper.toDto(entity, e -> {
            UtilisateurDto dto = new UtilisateurDto();
            dto.setId(e.getId());
            dto.setEmail(e.getEmail());
            dto.setNom(e.getNom());
            dto.setPrenom(e.getPrenom());
            dto.setActif(e.isActif());
            dto.setDateCreation(e.getDate_creation());
            dto.setDateModification(e.getDate_modification());
            if (e.getRole() != null) {
                RoleDto roleDto = new RoleDto();
                roleDto.setId(e.getRole().getId());
                roleDto.setCode(e.getRole().getCode());
                roleDto.setLabel(e.getRole().getLibelle());
                dto.setRole(roleDto);
            }
            return dto;
        });
    }

    public UtilisateurEntity toEntity(UtilisateurCreatePayload payload, RoleEntity role, String motDePasseHache) {
        UtilisateurEntity entity = new UtilisateurEntity();
        entity.setEmail(payload.getEmail());
        entity.setNom(payload.getNom());
        entity.setPrenom(payload.getPrenom() != null ? payload.getPrenom() : "");
        entity.setMotDePasse(motDePasseHache);
        entity.setRole(role);
        entity.setActif(false);
        return entity;
    }
}
