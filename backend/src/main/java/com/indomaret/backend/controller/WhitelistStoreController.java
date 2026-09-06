package com.indomaret.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.indomaret.backend.dto.ApiResponse;
import com.indomaret.backend.dto.WhitelistStoreRequest;
import com.indomaret.backend.dto.WhitelistStoreResponse;
import com.indomaret.backend.entity.User;
import com.indomaret.backend.security.CustomUserDetailsService;
import com.indomaret.backend.service.WhitelistStoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/whitelist-stores", "/api/whitelist"})
@RequiredArgsConstructor
@Tag(name = "Whitelist Store Management", description = "Endpoint untuk mendaftarkan dan mengelola toko whitelist")
public class WhitelistStoreController {

    private final WhitelistStoreService whitelistStoreService;
    private final CustomUserDetailsService userDetailsService;

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userDetailsService.getUserEntity(authentication.getName());
    }

    @PostMapping
    @Operation(summary = "Add Store to Whitelist", description = "Mendaftarkan toko ke whitelist (dibatasi kuota konfigurasi max-store-count)")
    public ResponseEntity<ApiResponse<WhitelistStoreResponse>> addStoreToWhitelist(
            @Valid @RequestBody WhitelistStoreRequest request,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WhitelistStoreResponse response = whitelistStoreService.addStoreToWhitelist(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Toko berhasil ditambahkan ke whitelist", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Toggle Whitelist Status", description = "Mengubah status aktif/nonaktif whitelist store")
    public ResponseEntity<ApiResponse<WhitelistStoreResponse>> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WhitelistStoreResponse response = whitelistStoreService.toggleWhitelistStatus(id, isActive, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Status whitelist berhasil diperbarui", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove Store from Whitelist", description = "Menghapus toko dari daftar whitelist")
    public ResponseEntity<ApiResponse<Void>> removeStoreFromWhitelist(
            @PathVariable Long id,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        whitelistStoreService.removeStoreFromWhitelist(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Toko berhasil dihapus dari whitelist", null));
    }

    @GetMapping
    @Operation(summary = "Get All Whitelist Stores", description = "Melihat semua toko yang saat ini berstatus aktif di whitelist")
    public ResponseEntity<ApiResponse<List<WhitelistStoreResponse>>> getAllActiveWhitelistStores() {
        List<WhitelistStoreResponse> response = whitelistStoreService.getAllActiveWhitelistStores();
        return ResponseEntity.ok(ApiResponse.ok("Daftar toko whitelist aktif", response));
    }
}
