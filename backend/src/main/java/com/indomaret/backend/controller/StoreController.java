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
               description = "Mencari toko berdasarkan nama provinsi dengan pagination. Hasil otomatis menggabungkan toko Whitelist dan dapat diurutkan berdasarkan created date (asc/desc).")
    public ResponseEntity<ApiResponse<PagedResponse<StoreResponse>>> searchStores(
            @RequestParam(required = false, defaultValue = "") String province,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @io.swagger.v3.oas.annotations.Parameter(description = "Urutan created date: 'asc' (terlama) atau 'desc' (terbaru). Default: 'desc'")
            @RequestParam(required = false, defaultValue = "desc") String sortDirection) {

        int pageSize = (size != null && size > 0) ? size : appProperties.getPagination().getDefaultPageSize();
        if (pageSize > appProperties.getPagination().getMaxPageSize()) {
            pageSize = appProperties.getPagination().getMaxPageSize();
        }

        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) 
                ? org.springframework.data.domain.Sort.Direction.ASC 
                : org.springframework.data.domain.Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, pageSize, org.springframework.data.domain.Sort.by(direction, "createdAt"));
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
