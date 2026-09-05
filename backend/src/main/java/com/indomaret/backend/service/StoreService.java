package com.indomaret.backend.service;

import org.springframework.data.domain.Pageable;

import com.indomaret.backend.dto.PagedResponse;
import com.indomaret.backend.dto.StoreResponse;

public interface StoreService {

    /**
     * Requirement Inti:
     * Pencarian toko berdasarkan nama provinsi + menggabungkan toko dari daftar Whitelist
     */
    PagedResponse<StoreResponse> searchStores(String provinceName, Pageable pageable);

    StoreResponse getStoreById(Long id);
}
