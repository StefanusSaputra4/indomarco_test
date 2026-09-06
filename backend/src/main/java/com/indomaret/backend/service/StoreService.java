package com.indomaret.backend.service;

import org.springframework.data.domain.Pageable;

import com.indomaret.backend.dto.PagedResponse;
import com.indomaret.backend.dto.StoreResponse;

public interface StoreService {

    PagedResponse<StoreResponse> searchStores(String provinceName, Pageable pageable);

    StoreResponse getStoreById(Long id);
}
