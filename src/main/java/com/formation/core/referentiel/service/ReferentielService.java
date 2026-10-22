package com.formation.core.referentiel.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.formation.core.referentiel.dto.CategorieDto;
import com.formation.core.referentiel.dto.RoleDto;
import com.formation.core.referentiel.dto.StatutCommandeDto;
import com.formation.core.referentiel.mapper.ReferencielMapper;
import com.formation.engine.repositories.CategorieRepository;
import com.formation.engine.repositories.RoleRepository;
import com.formation.engine.repositories.StatutCommandeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReferentielService implements IReferentielService {

    private final CategorieRepository categorieRepository;
    private final StatutCommandeRepository statutCommandeRepository;
    private final RoleRepository roleRepository;
    private final ReferencielMapper referencielMapper;

    @Override
    public List<CategorieDto> listCategories() {
        return referencielMapper.toCategoryDtoList(categorieRepository.findAll());
    }

    @Override
    public List<StatutCommandeDto> listStatutsCommande() {
        return referencielMapper.toStatutCommandeDtoList(statutCommandeRepository.findAll());
    }

    @Override
    public List<RoleDto> listRoles() {
        return referencielMapper.toRoleDtoList(roleRepository.findAll());
    }
}
