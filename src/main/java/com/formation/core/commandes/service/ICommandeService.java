package com.formation.core.commandes.service;

import org.springframework.lang.NonNull;

import com.formation.core.commandes.dto.CommandeCreatePayload;
import com.formation.core.commandes.dto.CommandeDto;
import com.formation.core.commandes.dto.CommandeListResponse;

public interface ICommandeService {

    CommandeDto creerCommande(CommandeCreatePayload payload, @NonNull Long utilisateurId);

    CommandeDto getCommandeById(@NonNull Long id);

    CommandeListResponse listCommandesByUtilisateur(@NonNull Long utilisateurId);

    CommandeDto changerStatut(@NonNull Long id, @NonNull Long statutId);

    void deleteCommande(@NonNull Long id);
}
