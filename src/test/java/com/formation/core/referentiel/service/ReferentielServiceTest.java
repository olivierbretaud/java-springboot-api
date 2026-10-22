package com.formation.core.referentiel.service;

import com.formation.core.referentiel.dto.CategorieDto;
import com.formation.core.referentiel.dto.RoleDto;
import com.formation.core.referentiel.dto.StatutCommandeDto;
import com.formation.core.referentiel.mapper.ReferencielMapper;
import com.formation.engine.entities.CategorieEntity;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.StatutCommandeEntity;
import com.formation.engine.repositories.CategorieRepository;
import com.formation.engine.repositories.RoleRepository;
import com.formation.engine.repositories.StatutCommandeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferentielServiceTest {

    @Mock
    private CategorieRepository categorieRepository;

    @Mock
    private StatutCommandeRepository statutCommandeRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ReferencielMapper referencielMapper;

    @InjectMocks
    private ReferentielService referentielService;

    @Test
    void listCategories_retourne_liste_mappee() {
        CategorieEntity entity = new CategorieEntity();
        entity.setId(1L);
        entity.setNom("Légumes");

        CategorieDto dto = new CategorieDto();
        dto.setId(1L);
        dto.setLabel("Légumes");

        when(categorieRepository.findAll()).thenReturn(List.of(entity));
        when(referencielMapper.toCategoryDtoList(List.of(entity))).thenReturn(List.of(dto));

        List<CategorieDto> result = referentielService.listCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getLabel()).isEqualTo("Légumes");
    }

    @Test
    void listCategories_retourne_liste_vide_si_repository_vide() {
        when(categorieRepository.findAll()).thenReturn(List.of());
        when(referencielMapper.toCategoryDtoList(List.of())).thenReturn(List.of());

        List<CategorieDto> result = referentielService.listCategories();

        assertThat(result).isEmpty();
    }

    // ── StatutCommande ────────────────────────────────────────────────────────

    @Test
    void listStatutsCommande_retourne_liste_mappee() {
        StatutCommandeEntity entity = new StatutCommandeEntity();
        entity.setId(1L);
        entity.setLibelle("En attente");

        StatutCommandeDto dto = new StatutCommandeDto();
        dto.setId(1L);
        dto.setLabel("En attente");

        when(statutCommandeRepository.findAll()).thenReturn(List.of(entity));
        when(referencielMapper.toStatutCommandeDtoList(List.of(entity))).thenReturn(List.of(dto));

        List<StatutCommandeDto> result = referentielService.listStatutsCommande();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getLabel()).isEqualTo("En attente");
    }

    @Test
    void listStatutsCommande_retourne_liste_vide_si_repository_vide() {
        when(statutCommandeRepository.findAll()).thenReturn(List.of());
        when(referencielMapper.toStatutCommandeDtoList(List.of())).thenReturn(List.of());

        List<StatutCommandeDto> result = referentielService.listStatutsCommande();

        assertThat(result).isEmpty();
    }

    // ── Roles ─────────────────────────────────────────────────────────────────

    @Test
    void listRoles_retourne_liste_mappee() {
        RoleEntity entity = new RoleEntity();
        entity.setId(1L);
        entity.setLibelle("Admin");

        RoleDto dto = new RoleDto();
        dto.setId(1L);
        dto.setLabel("Admin");

        when(roleRepository.findAll()).thenReturn(List.of(entity));
        when(referencielMapper.toRoleDtoList(List.of(entity))).thenReturn(List.of(dto));

        List<RoleDto> result = referentielService.listRoles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getLabel()).isEqualTo("Admin");
    }

    @Test
    void listRoles_retourne_liste_vide_si_repository_vide() {
        when(roleRepository.findAll()).thenReturn(List.of());
        when(referencielMapper.toRoleDtoList(List.of())).thenReturn(List.of());

        List<RoleDto> result = referentielService.listRoles();

        assertThat(result).isEmpty();
    }
}
