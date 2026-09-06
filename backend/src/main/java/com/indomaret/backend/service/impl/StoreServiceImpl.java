package com.indomaret.backend.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.indomaret.backend.dto.PagedResponse;
import com.indomaret.backend.dto.StoreResponse;
import com.indomaret.backend.entity.Store;
import com.indomaret.backend.entity.WhitelistStore;
import com.indomaret.backend.exception.ResourceNotFoundException;
import com.indomaret.backend.repository.StoreRepository;
import com.indomaret.backend.repository.WhitelistStoreRepository;
import com.indomaret.backend.service.StoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final WhitelistStoreRepository whitelistStoreRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StoreResponse> searchStores(String provinceName, Pageable pageable) {
        String query = provinceName != null ? provinceName.trim() : "";

        Page<Store> storePage = storeRepository.searchByProvinceName(query, pageable);
        List<WhitelistStore> activeWhitelists = new ArrayList<>(whitelistStoreRepository.findAllActiveWithDetails());

        // Urutkan toko whitelist sesuai arah sort yang diminta (default created date)
        if (pageable.getSort() != null && pageable.getSort().isSorted()) {
            boolean isAsc = pageable.getSort().stream().anyMatch(org.springframework.data.domain.Sort.Order::isAscending);
            activeWhitelists.sort((w1, w2) -> {
                java.time.LocalDateTime d1 = (w1.getStore() != null) ? w1.getStore().getCreatedAt() : null;
                java.time.LocalDateTime d2 = (w2.getStore() != null) ? w2.getStore().getCreatedAt() : null;
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return isAsc ? d1.compareTo(d2) : d2.compareTo(d1);
            });
        }

        List<StoreResponse> resultList = new ArrayList<>();
        Set<Long> alreadyIncludedStoreIds = new HashSet<>();

        // 1. Whitelist stores are ALWAYS placed at the top of results
        for (WhitelistStore ws : activeWhitelists) {
            Store whitelistedStore = ws.getStore();
            if (whitelistedStore != null) {
                resultList.add(mapToResponse(whitelistedStore, true));
                alreadyIncludedStoreIds.add(whitelistedStore.getId());
            }
        }

        // 2. Regular stores matching search criteria are appended next
        for (Store store : storePage.getContent()) {
            if (!alreadyIncludedStoreIds.contains(store.getId())) {
                resultList.add(mapToResponse(store, false));
                alreadyIncludedStoreIds.add(store.getId());
            }
        }

        log.debug("Store search for province '{}': found {} stores, merged {} whitelisted stores",
                provinceName, storePage.getNumberOfElements(), activeWhitelists.size());

        return PagedResponse.<StoreResponse>builder()
                .content(resultList)
                .page(storePage.getNumber())
                .size(resultList.size())
                .totalElements(storePage.getTotalElements() + (resultList.size() - storePage.getNumberOfElements()))
                .totalPages(storePage.getTotalPages())
                .last(storePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {
        Store store = storeRepository.findActiveByIdWithBranchAndProvince(id)
                .orElseThrow(() -> new ResourceNotFoundException("Toko tidak ditemukan dengan ID: " + id));

        boolean isWhitelisted = whitelistStoreRepository.existsByStoreIdAndIsActiveTrue(store.getId());
        return mapToResponse(store, isWhitelisted);
    }

    private StoreResponse mapToResponse(Store store, boolean isWhitelisted) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .branchId(store.getBranch() != null ? store.getBranch().getId() : null)
                .branchName(store.getBranch() != null ? store.getBranch().getName() : null)
                .provinceId(store.getBranch() != null && store.getBranch().getProvince() != null 
                        ? store.getBranch().getProvince().getId() : null)
                .provinceName(store.getBranch() != null && store.getBranch().getProvince() != null 
                        ? store.getBranch().getProvince().getName() : null)
                .createdAt(store.getCreatedAt())
                .whitelisted(isWhitelisted)
                .build();
    }
}
