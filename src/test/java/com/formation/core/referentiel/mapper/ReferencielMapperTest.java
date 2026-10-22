package com.formation.core.referentiel.mapper;

import com.formation.core.referentiel.dto.CategorieDto;
import com.formation.core.referentiel.dto.RoleDto;
import com.formation.core.referentiel.dto.StatutCommandeDto;
import com.formation.engine.entities.CategorieEntity;
import com.formation.engine.entities.StatutCommandeEntity;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.utils.EntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReferencielMapperTest {

    private ReferencielMapper referencielMapper;

    @BeforeEach
    void setUp() {
        referencielMapper = new ReferencielMapper(new EntityMapper());
    }

    @Test
    void toCategoryDto_mappe_correctement_entity_vers_dto() {
        CategorieEntity entity = new CategorieEntity();
        entity.setId(1L);
        entity.setNom("Fruits");

        CategorieDto dto = referencielMapper.toCategoryDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getLabel()).isEqualTo("Fruits");
    }

    @Test
    void toCategoryDto_retourne_null_si_entity_null() {
        CategorieDto dto = referencielMapper.toCategoryDto(null);

        assertThat(dto).isNull();
    }

    @Test
    void toCategoryDtoList_mappe_liste_entities() {
        CategorieEntity e1 = new CategorieEntity();
        e1.setId(1L);
        e1.setNom("Fruits");

        CategorieEntity e2 = new CategorieEntity();
        e2.setId(2L);
        e2.setNom("Légumes");

        List<CategorieDto> dtos = referencielMapper.toCategoryDtoList(List.of(e1, e2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getLabel()).isEqualTo("Fruits");
        assertThat(dtos.get(1).getLabel()).isEqualTo("Légumes");
    }

    @Test
    void toCategoryDtoList_retourne_liste_vide_si_liste_nulle() {
        List<CategorieDto> dtos = referencielMapper.toCategoryDtoList(null);

        assertThat(dtos).isEmpty();
    }

    // ── StatutCommandeDto ─────────────────────────────────────────────────────

    @Test
    void toStatutCommandeDto_mappe_id_et_libelle() {
        StatutCommandeEntity entity = new StatutCommandeEntity();
        entity.setId(1L);
        entity.setCode("EN_ATTENTE");
        entity.setLibelle("En attente");

        StatutCommandeDto dto = referencielMapper.toStatutCommandeDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getLabel()).isEqualTo("En attente");
    }

    @Test
    void toStatutCommandeDto_retourne_null_si_entity_null() {
        assertThat(referencielMapper.toStatutCommandeDto(null)).isNull();
    }

    @Test
    void toStatutCommandeDtoList_mappe_liste_entities() {
        StatutCommandeEntity e1 = new StatutCommandeEntity();
        e1.setId(1L);
        e1.setCode("EN_ATTENTE");
        e1.setLibelle("En attente");

        StatutCommandeEntity e2 = new StatutCommandeEntity();
        e2.setId(2L);
        e2.setCode("CONFIRMEE");
        e2.setLibelle("Confirmée");

        List<StatutCommandeDto> dtos = referencielMapper.toStatutCommandeDtoList(List.of(e1, e2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo(1L);
        assertThat(dtos.get(0).getLabel()).isEqualTo("En attente");
        assertThat(dtos.get(1).getId()).isEqualTo(2L);
        assertThat(dtos.get(1).getLabel()).isEqualTo("Confirmée");
    }

    @Test
    void toStatutCommandeDtoList_retourne_liste_vide_si_liste_nulle() {
        List<StatutCommandeDto> dtos = referencielMapper.toStatutCommandeDtoList(null);

        assertThat(dtos).isEmpty();
    }

    @Test
    void toRoleDto_mappe_id_et_libelle() {
        RoleEntity entity = new RoleEntity();
        entity.setId(1L);
        entity.setLibelle("Membre");

        RoleDto dto = referencielMapper.toRoleDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getLabel()).isEqualTo("Membre");
    }

    @Test
    void toRoleDtoList_mappe_liste_entities() {
        RoleEntity e1 = new RoleEntity();
        e1.setId(1L);
        e1.setLibelle("Membre");

        RoleEntity e2 = new RoleEntity();
        e2.setId(2L);
        e2.setLibelle("Admin");

        List<RoleDto> dtos = referencielMapper.toRoleDtoList(List.of(e1, e2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getLabel()).isEqualTo("Membre");
        assertThat(dtos.get(1).getLabel()).isEqualTo("Admin");
    }

    @Test
    void toRoleDtoList_retourne_liste_vide_si_liste_nulle() {
        List<RoleDto> dtos = referencielMapper.toRoleDtoList(null);

        assertThat(dtos).isEmpty();
    }
}
