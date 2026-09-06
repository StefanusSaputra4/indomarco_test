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
import org.springframework.web.bind.annotation.RestController;

import com.indomaret.backend.dto.ApiResponse;
import com.indomaret.backend.dto.BranchRequest;
import com.indomaret.backend.dto.BranchResponse;
import com.indomaret.backend.entity.User;
import com.indomaret.backend.security.CustomUserDetailsService;
import com.indomaret.backend.service.BranchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@Tag(name = "Branch Management", description = "Endpoint untuk pengelolaan cabang, update, dan soft-delete")
public class BranchController {

    private final BranchService branchService;
    private final CustomUserDetailsService userDetailsService;

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userDetailsService.getUserEntity(authentication.getName());
    }

    @PostMapping
    @Operation(summary = "Create Branch", description = "Membuat kantor cabang baru")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @Valid @RequestBody BranchRequest request, 
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        BranchResponse response = branchService.createBranch(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cabang berhasil dibuat", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Branch", description = "Mengubah data cabang. Perubahan otomatis dicatat ke Audit Log.")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchRequest request,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        BranchResponse response = branchService.updateBranch(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Cabang berhasil diperbarui", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft Delete Branch", description = "Menghapus cabang secara Soft Delete (is_active=false, deleted_at=now). Aksi dicatat ke Audit Log.")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(
            @PathVariable Long id,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        branchService.deleteBranch(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Cabang berhasil dihapus (soft delete)", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Branch by ID", description = "Mengambil detail cabang aktif berdasarkan ID")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(@PathVariable Long id) {
        BranchResponse response = branchService.getBranchById(id);
        return ResponseEntity.ok(ApiResponse.ok("Data cabang ditemukan", response));
    }

    @GetMapping
    @Operation(summary = "Get All Branches", description = "Mengambil seluruh daftar cabang yang aktif. Dapat diurutkan berdasarkan created date (asc/desc).")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllActiveBranches(
            @io.swagger.v3.oas.annotations.Parameter(description = "Urutan created date: 'asc' (terlama) atau 'desc' (terbaru). Default: 'asc'")
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "asc") String sortDirection) {
        List<BranchResponse> response = branchService.getAllActiveBranches();
        if ("desc".equalsIgnoreCase(sortDirection)) {
            response.sort((b1, b2) -> {
                if (b1.getCreatedAt() == null && b2.getCreatedAt() == null) return 0;
                if (b1.getCreatedAt() == null) return 1;
                if (b2.getCreatedAt() == null) return -1;
                return b2.getCreatedAt().compareTo(b1.getCreatedAt());
            });
        } else if ("asc".equalsIgnoreCase(sortDirection)) {
            response.sort((b1, b2) -> {
                if (b1.getCreatedAt() == null && b2.getCreatedAt() == null) return 0;
                if (b1.getCreatedAt() == null) return 1;
                if (b2.getCreatedAt() == null) return -1;
                return b1.getCreatedAt().compareTo(b2.getCreatedAt());
            });
        }
        return ResponseEntity.ok(ApiResponse.ok("Daftar cabang aktif", response));
    }
}
