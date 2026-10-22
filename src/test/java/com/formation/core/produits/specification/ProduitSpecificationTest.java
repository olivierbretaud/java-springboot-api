package com.formation.core.produits.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.formation.core.produits.dto.ProduitSearchParams;
import com.formation.engine.entities.ProduitEntity;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@SuppressWarnings("unchecked")
class ProduitSpecificationTest {

    private Root<ProduitEntity> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;

    private Path<String> nomPath;
    private Path<String> descriptionPath;
    private Path<BigDecimal> prixPath;
    private Path<Object> categoriePath;
    private Path<Object> categorieIdPath;

    private Expression<String> lowerNom;
    private Expression<String> lowerDesc;

    private Predicate likePredicate;
    private Predicate orPredicate;
    private Predicate andPredicate;
    private Predicate equalPredicate;
    private Predicate gePredicate;
    private Predicate lePredicate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);

        nomPath = mock(Path.class);
        descriptionPath = mock(Path.class);
        prixPath = mock(Path.class);
        categoriePath = mock(Path.class);
        categorieIdPath = mock(Path.class);

        lowerNom = mock(Expression.class);
        lowerDesc = mock(Expression.class);

        likePredicate = mock(Predicate.class);
        orPredicate = mock(Predicate.class);
        andPredicate = mock(Predicate.class);
        equalPredicate = mock(Predicate.class);
        gePredicate = mock(Predicate.class);
        lePredicate = mock(Predicate.class);

        when(root.<String>get("nom")).thenReturn(nomPath);
        when(root.<String>get("description")).thenReturn(descriptionPath);
        when(root.<BigDecimal>get("prix")).thenReturn(prixPath);
        when(root.get("categorie")).thenReturn(categoriePath);
        when(categoriePath.get("id")).thenReturn(categorieIdPath);

        when(cb.lower(nomPath)).thenReturn(lowerNom);
        when(cb.lower(descriptionPath)).thenReturn(lowerDesc);
        when(cb.like(any(Expression.class), anyString())).thenReturn(likePredicate);
        when(cb.or(any(Predicate[].class))).thenReturn(orPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);
        when(cb.equal(any(), any())).thenReturn(equalPredicate);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(gePredicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(lePredicate);
    }

    @Test
    void from_retourne_specification_non_null() {
        ProduitSearchParams params = new ProduitSearchParams();

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);

        assertThat(spec).isNotNull();
    }

    @Test
    void from_sans_params_retourne_predicate_vide() {
        ProduitSearchParams params = new ProduitSearchParams();

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);
        spec.toPredicate(root, query, cb);

        verify(cb, never()).like(any(Expression.class), anyString());
        verify(cb, never()).equal(any(), any());
        verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(BigDecimal.class));
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(BigDecimal.class));
        verify(cb).and(new Predicate[0]);
    }

    @Test
    void from_avec_search_ajoute_predicate_like_sur_nom_et_description() {
        ProduitSearchParams params = new ProduitSearchParams();
        params.setSearch("pomme");

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);
        spec.toPredicate(root, query, cb);

        verify(cb).lower(nomPath);
        verify(cb).lower(descriptionPath);
        verify(cb).like(lowerNom, "%pomme%");
        verify(cb).like(lowerDesc, "%pomme%");
        verify(cb).or(likePredicate, likePredicate);
    }

    @Test
    void from_avec_search_blank_n_ajoute_pas_predicate() {
        ProduitSearchParams params = new ProduitSearchParams();
        params.setSearch("   ");

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);
        spec.toPredicate(root, query, cb);

        verify(cb, never()).like(any(Expression.class), anyString());
    }

    @Test
    void from_avec_search_convertit_en_minuscules() {
        ProduitSearchParams params = new ProduitSearchParams();
        params.setSearch("POMME");

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);
        spec.toPredicate(root, query, cb);

        verify(cb).like(lowerNom, "%pomme%");
        verify(cb).like(lowerDesc, "%pomme%");
    }

    @Test
    void from_avec_categorieId_ajoute_predicate_equal() {
        ProduitSearchParams params = new ProduitSearchParams();
        params.setCategorieId(5L);

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);
        spec.toPredicate(root, query, cb);

        verify(cb).equal(categorieIdPath, 5L);
    }

    @Test
    void from_avec_prixMin_ajoute_predicate_greaterThanOrEqual() {
        ProduitSearchParams params = new ProduitSearchParams();
        params.setPrixMin(BigDecimal.valueOf(10.0));

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);
        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(prixPath, BigDecimal.valueOf(10.0));
    }

    @Test
    void from_avec_prixMax_ajoute_predicate_lessThanOrEqual() {
        ProduitSearchParams params = new ProduitSearchParams();
        params.setPrixMax(BigDecimal.valueOf(50.0));

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);
        spec.toPredicate(root, query, cb);

        verify(cb).lessThanOrEqualTo(prixPath, BigDecimal.valueOf(50.0));
    }

    @Test
    void from_avec_tous_les_params_ajoute_tous_les_predicates() {
        ProduitSearchParams params = new ProduitSearchParams();
        params.setSearch("bio");
        params.setCategorieId(2L);
        params.setPrixMin(BigDecimal.valueOf(5.0));
        params.setPrixMax(BigDecimal.valueOf(100.0));

        Specification<ProduitEntity> spec = ProduitSpecification.from(params);
        spec.toPredicate(root, query, cb);

        verify(cb).like(lowerNom, "%bio%");
        verify(cb).like(lowerDesc, "%bio%");
        verify(cb).equal(categorieIdPath, 2L);
        verify(cb).greaterThanOrEqualTo(prixPath, BigDecimal.valueOf(5.0));
        verify(cb).lessThanOrEqualTo(prixPath, BigDecimal.valueOf(100.0));
    }
}
