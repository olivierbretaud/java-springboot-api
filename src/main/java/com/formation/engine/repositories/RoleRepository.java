package com.formation.engine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.formation.engine.entities.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
}
