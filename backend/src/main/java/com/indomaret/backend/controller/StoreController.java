package com.indomaret.backend.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.indomaret.backend.config.AppProperties;
import com.indomaret.backend.dto.ApiResponse;
import com.indomaret.backend.dto.PagedResponse;
import com.indomaret.backend.dto.StoreResponse;
import com.indomaret.backend.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@Tag(name = "Store Management", description = "Endpoint untuk pencarian toko dan detail toko")
public class StoreController {

    private final StoreService storeService;
    private final AppProperties appProperties;

    @GetMapping("/search")
    @Operation(summary = "Search Store by Province", 
               description = "Mencari toko berdasarkan nama provinsi dengan pagination. Hasil otomatis menggabungkan toko Whitelist.")
    public ResponseEntity<ApiResponse<PagedResponse<StoreResponse>>> searchStores(
            @RequestParam(required = false, defaultValue = "") String province,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {

        int pageSize = (size != null && size > 0) ? size : appProperties.getPagination().getDefaultPageSize();
        // Batasi ukuran page maksimum dari konfigurasi
        if (pageSize > appProperties.getPagination().getMaxPageSize()) {
            pageSize = appProperties.getPagination().getMaxPageSize();
        }

        Pageable pageable = PageRequest.of(page, pageSize);
        PagedResponse<StoreResponse> result = storeService.searchStores(province, pageable);

        return ResponseEntity.ok(ApiResponse.ok("Pencarian toko berhasil", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Store by ID", description = "Mengambil detail toko berdasarkan ID toko")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreById(@PathVariable Long id) {
        StoreResponse response = storeService.getStoreById(id);
        return ResponseEntity.ok(ApiResponse.ok("Data toko ditemukan", response));
    }
}
