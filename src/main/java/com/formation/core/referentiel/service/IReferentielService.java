package com.formation.core.referentiel.service;

import java.util.List;

import com.formation.core.referentiel.dto.CategorieDto;
import com.formation.core.referentiel.dto.RoleDto;
import com.formation.core.referentiel.dto.StatutCommandeDto;

public interface IReferentielService {

    List<CategorieDto> listCategories();

    List<StatutCommandeDto> listStatutsCommande();

    List<RoleDto> listRoles();
}