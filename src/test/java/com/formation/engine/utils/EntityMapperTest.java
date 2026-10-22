package com.formation.engine.utils;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperTest {

    private EntityMapper entityMapper;

    @BeforeEach
    void setUp() {
        entityMapper = new EntityMapper();
    }

    @Test
    void toEntity_mappe_correctement_dto_vers_entity() {
        String dto = "Pomme";

        String result = entityMapper.toEntity(dto, String::toUpperCase);

        assertThat(result).isEqualTo("POMME");
    }

    @Test
    void toEntity_retourne_null_si_dto_null() {
        String result = entityMapper.<String, String>toEntity(null, String::toUpperCase);

        assertThat(result).isNull();
    }

    @Test
    void toDto_mappe_correctement_entity_vers_dto() {
        String entity = "pomme";

        String result = entityMapper.toDto(entity, String::toUpperCase);

        assertThat(result).isEqualTo("POMME");
    }

    @Test
    void toDto_retourne_null_si_entity_null() {
        String result = entityMapper.<String, String>toDto(null, String::toUpperCase);

        assertThat(result).isNull();
    }

    @Test
    void toDtoList_mappe_liste_entities() {
        List<String> entities = List.of("pomme", "poire");

        List<String> result = entityMapper.toDtoList(entities, String::toUpperCase);

        assertThat(result).containsExactly("POMME", "POIRE");
    }

    @Test
    void toDtoList_retourne_liste_vide_si_liste_nulle() {
        List<String> result = entityMapper.<String, String>toDtoList(null, String::toUpperCase);

        assertThat(result).isEmpty();
    }

    @Test
    void toDtoList_retourne_liste_vide_si_liste_vide() {
        List<String> result = entityMapper.toDtoList(List.<String>of(), String::toUpperCase);

        assertThat(result).isEmpty();
    }
}
