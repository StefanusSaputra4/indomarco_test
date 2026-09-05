package com.indomaret.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.indomaret.backend.entity.Branch;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    @Query("SELECT b FROM Branch b JOIN FETCH b.province p WHERE b.id = :id AND b.isActive = true AND b.deletedAt IS NULL")
    Optional<Branch> findActiveByIdWithProvince(@Param("id") Long id);

    List<Branch> findAllByIsActiveTrueAndDeletedAtIsNull();

    List<Branch> findByProvinceIdAndIsActiveTrueAndDeletedAtIsNull(Long provinceId);
}
