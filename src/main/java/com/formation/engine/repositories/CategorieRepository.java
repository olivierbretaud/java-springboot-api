package com.formation.engine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.formation.engine.entities.CategorieEntity;

@Repository
public interface CategorieRepository extends JpaRepository<CategorieEntity, Long> {

}


