package com.indomaret.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.indomaret.backend.entity.Province;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    List<Province> findAllByIsActiveTrueAndDeletedAtIsNull();

    Optional<Province> findByNameIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull(String name);

    boolean existsByCodeIgnoreCase(String code);
}
