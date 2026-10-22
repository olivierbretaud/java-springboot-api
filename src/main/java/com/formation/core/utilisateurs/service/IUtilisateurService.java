package com.formation.core.utilisateurs.service;

import com.formation.core.utilisateurs.dto.UtilisateurCreatePayload;
import com.formation.core.utilisateurs.dto.UtilisateurDto;
import com.formation.core.utilisateurs.dto.UtilisateurUpdatePayload;
import com.formation.engine.pagination.PageParams;
import com.formation.engine.pagination.PageResponse;

public interface IUtilisateurService {

    PageResponse<UtilisateurDto> listUtilisateurs(PageParams params);

    UtilisateurDto getUtilisateurById(Long id);

    UtilisateurDto createUtilisateur(UtilisateurCreatePayload payload);

    UtilisateurDto updateUtilisateur(Long id, UtilisateurUpdatePayload payload);
}
