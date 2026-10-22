package com.formation.engine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.formation.engine.entities.ProduitEntity;

@Repository
public interface ProduitRepository extends JpaRepository<ProduitEntity, Long>, JpaSpecificationExecutor<ProduitEntity> {

}


