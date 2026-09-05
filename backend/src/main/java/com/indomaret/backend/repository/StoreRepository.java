package com.indomaret.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.indomaret.backend.entity.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    @Query("SELECT s FROM Store s JOIN FETCH s.branch b JOIN FETCH b.province p " +
           "WHERE s.id = :id AND s.isActive = true AND s.deletedAt IS NULL")
    Optional<Store> findActiveByIdWithBranchAndProvince(@Param("id") Long id);

    @Query(value = "SELECT s FROM Store s " +
                   "JOIN FETCH s.branch b " +
                   "JOIN FETCH b.province p " +
                   "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :provinceName, '%')) " +
                   "AND s.isActive = true AND s.deletedAt IS NULL " +
                   "AND b.isActive = true AND b.deletedAt IS NULL " +
                   "AND p.isActive = true AND p.deletedAt IS NULL",
           countQuery = "SELECT COUNT(s) FROM Store s " +
                        "JOIN s.branch b " +
                        "JOIN b.province p " +
                        "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :provinceName, '%')) " +
                        "AND s.isActive = true AND s.deletedAt IS NULL " +
                        "AND b.isActive = true AND b.deletedAt IS NULL " +
                        "AND p.isActive = true AND p.deletedAt IS NULL")
    Page<Store> searchByProvinceName(@Param("provinceName") String provinceName, Pageable pageable);

    @Query("SELECT s FROM Store s " +
           "JOIN FETCH s.branch b " +
           "JOIN FETCH b.province p " +
           "WHERE s.id IN :storeIds " +
           "AND s.isActive = true AND s.deletedAt IS NULL")
    List<Store> findAllActiveWithDetailsByIdIn(@Param("storeIds") Collection<Long> storeIds);
}
