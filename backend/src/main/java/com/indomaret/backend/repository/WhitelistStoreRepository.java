package com.indomaret.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.indomaret.backend.entity.WhitelistStore;

@Repository
public interface WhitelistStoreRepository extends JpaRepository<WhitelistStore, Long> {

    @Query("SELECT ws FROM WhitelistStore ws " +
           "JOIN FETCH ws.store s " +
           "JOIN FETCH s.branch b " +
           "JOIN FETCH b.province p " +
           "WHERE ws.isActive = true AND s.isActive = true AND s.deletedAt IS NULL " +
           "AND b.isActive = true AND b.deletedAt IS NULL " +
           "AND p.isActive = true AND p.deletedAt IS NULL")
    List<WhitelistStore> findAllActiveWithDetails();

    boolean existsByStoreIdAndIsActiveTrue(Long storeId);

    long countByIsActiveTrue();

    Optional<WhitelistStore> findByStoreId(Long storeId);
}
