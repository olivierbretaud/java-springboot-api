package com.formation.core.utilisateurs.service;

import com.formation.core.utilisateurs.dto.UtilisateurCreatePayload;
import com.formation.core.utilisateurs.dto.UtilisateurDto;
import com.formation.core.utilisateurs.dto.UtilisateurUpdatePayload;
import com.formation.engine.pagination.PageParams;
import com.formation.engine.pagination.PageResponse;
import org.springframework.lang.NonNull;

public interface IUtilisateurService {

    PageResponse<UtilisateurDto> listUtilisateurs(PageParams params);

    UtilisateurDto getUtilisateurById(@NonNull Long id);

    UtilisateurDto createUtilisateur(UtilisateurCreatePayload payload);

    UtilisateurDto updateUtilisateur(@NonNull Long id, UtilisateurUpdatePayload payload);
}
