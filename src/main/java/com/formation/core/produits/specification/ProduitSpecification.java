package com.formation.core.produits.specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.formation.core.produits.dto.ProduitSearchParams;
import com.formation.engine.entities.ProduitEntity;

import jakarta.persistence.criteria.Predicate;

public class ProduitSpecification {

    private ProduitSpecification() {}

    public static Specification<ProduitEntity> from(ProduitSearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String search = params.getSearch();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("nom")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            Long categorieId = params.getCategorieId();
            if (categorieId != null) {
                predicates.add(cb.equal(root.get("categorie").get("id"), categorieId));
            }

            BigDecimal prixMin = params.getPrixMin();
            if (prixMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("prix"), prixMin));
            }

            BigDecimal prixMax = params.getPrixMax();
            if (prixMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("prix"), prixMax));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
