package com.formation.engine.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void constructor_mappe_les_champs_depuis_page() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<String> page = new PageImpl<>(List.of("a", "b"), pageable, 2L);

        PageResponse<String> response = new PageResponse<>(page);

        assertThat(response.getContenu()).containsExactly("a", "b");
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isDernierePage()).isTrue();
    }

    @Test
    void constructor_derniere_page_est_false_quand_plusieurs_pages() {
        PageRequest pageable = PageRequest.of(0, 1);
        Page<String> page = new PageImpl<>(List.of("a"), pageable, 5);

        PageResponse<String> response = new PageResponse<>(page);

        assertThat(response.isDernierePage()).isFalse();
        assertThat(response.getTotalPages()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(5);
    }

    @Test
    void constructor_page_vide_retourne_valeurs_zero() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<String> page = new PageImpl<>(List.of(), pageable, 0);

        PageResponse<String> response = new PageResponse<>(page);

        assertThat(response.getContenu()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
        assertThat(response.isDernierePage()).isTrue();
    }

    @Test
    void constructor_mappe_le_numero_de_page_courant() {
        PageRequest pageable = PageRequest.of(2, 5);
        Page<String> page = new PageImpl<>(List.of("a"), pageable, 15);

        PageResponse<String> response = new PageResponse<>(page);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isDernierePage()).isTrue();
    }
}
