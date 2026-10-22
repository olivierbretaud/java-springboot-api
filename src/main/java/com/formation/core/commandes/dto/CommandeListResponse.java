package com.formation.core.commandes.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommandeListResponse {

    private List<CommandeDto> commandes;
}
