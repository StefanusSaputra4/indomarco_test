package com.indomaret.backend.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.indomaret.backend.config.AppProperties;
import com.indomaret.backend.dto.WhitelistStoreRequest;
import com.indomaret.backend.dto.WhitelistStoreResponse;
import com.indomaret.backend.entity.Store;
import com.indomaret.backend.entity.User;
import com.indomaret.backend.entity.WhitelistStore;
import com.indomaret.backend.exception.BadRequestException;
import com.indomaret.backend.exception.ResourceNotFoundException;
import com.indomaret.backend.repository.StoreRepository;
import com.indomaret.backend.repository.WhitelistStoreRepository;
import com.indomaret.backend.service.AuditLogService;
import com.indomaret.backend.service.WhitelistStoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhitelistStoreServiceImpl implements WhitelistStoreService {

    private final WhitelistStoreRepository whitelistStoreRepository;
    private final StoreRepository storeRepository;
    private final AppProperties appProperties;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public WhitelistStoreResponse addStoreToWhitelist(WhitelistStoreRequest request, User currentUser) {
        long currentCount = whitelistStoreRepository.countByIsActiveTrue();
        int maxAllowed = appProperties.getWhitelist().getMaxStoreCount();
        if (currentCount >= maxAllowed) {
            throw new BadRequestException("Batas maksimal whitelist toko (" + maxAllowed + ") sudah tercapai!");
        }

        Store store = storeRepository.findActiveByIdWithBranchAndProvince(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Toko tidak ditemukan dengan ID: " + request.getStoreId()));

        if (whitelistStoreRepository.existsByStoreIdAndIsActiveTrue(store.getId())) {
            throw new BadRequestException("Toko ini sudah terdaftar dalam whitelist aktif!");
        }

        WhitelistStore whitelistStore = new WhitelistStore();
        whitelistStore.setStore(store);
        whitelistStore.setIsActive(true);

        WhitelistStore saved = whitelistStoreRepository.save(whitelistStore);
        auditLogService.logChange(currentUser, "WhitelistStore", saved.getId(), "CREATE", null, mapToResponse(saved));

        log.info("Store ID {} added to whitelist", store.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public WhitelistStoreResponse toggleWhitelistStatus(Long id, boolean isActive, User currentUser) {
        WhitelistStore whitelistStore = whitelistStoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data whitelist tidak ditemukan dengan ID: " + id));

        WhitelistStoreResponse oldState = mapToResponse(whitelistStore);

        whitelistStore.setIsActive(isActive);
        WhitelistStore updated = whitelistStoreRepository.save(whitelistStore);
        WhitelistStoreResponse newState = mapToResponse(updated);

        auditLogService.logChange(currentUser, "WhitelistStore", updated.getId(), "UPDATE", oldState, newState);
        return newState;
    }

    @Override
    @Transactional
    public void removeStoreFromWhitelist(Long id, User currentUser) {
        WhitelistStore whitelistStore = whitelistStoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data whitelist tidak ditemukan dengan ID: " + id));

        WhitelistStoreResponse oldState = mapToResponse(whitelistStore);
        whitelistStoreRepository.delete(whitelistStore);

        auditLogService.logChange(currentUser, "WhitelistStore", id, "DELETE", oldState, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhitelistStoreResponse> getAllActiveWhitelistStores() {
        return whitelistStoreRepository.findAllActiveWithDetails()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WhitelistStoreResponse mapToResponse(WhitelistStore ws) {
        Store store = ws.getStore();
        return WhitelistStoreResponse.builder()
                .id(ws.getId())
                .storeId(store != null ? store.getId() : null)
                .storeName(store != null ? store.getName() : null)
                .branchName(store != null && store.getBranch() != null ? store.getBranch().getName() : null)
                .provinceName(store != null && store.getBranch() != null && store.getBranch().getProvince() != null 
                        ? store.getBranch().getProvince().getName() : null)
                .isActive(ws.getIsActive())
                .createdAt(ws.getCreatedAt())
                .build();
    }
}
