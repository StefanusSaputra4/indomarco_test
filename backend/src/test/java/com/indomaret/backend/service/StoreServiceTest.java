package com.indomaret.backend.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.indomaret.backend.dto.PagedResponse;
import com.indomaret.backend.dto.StoreResponse;
import com.indomaret.backend.entity.Branch;
import com.indomaret.backend.entity.Province;
import com.indomaret.backend.entity.Store;
import com.indomaret.backend.entity.WhitelistStore;
import com.indomaret.backend.exception.ResourceNotFoundException;
import com.indomaret.backend.repository.StoreRepository;
import com.indomaret.backend.repository.WhitelistStoreRepository;
import com.indomaret.backend.service.impl.StoreServiceImpl;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private WhitelistStoreRepository whitelistStoreRepository;

    @InjectMocks
    private StoreServiceImpl storeService;

    private Province provinceJabar;
    private Province provinceJatim;
    private Branch branchBandung;
    private Branch branchSurabaya;
    private Store regularStore;
    private Store whitelistStore;
    private WhitelistStore activeWhitelist;

    @BeforeEach
    void setUp() {
        provinceJabar = new Province();
        provinceJabar.setId(1L);
        provinceJabar.setName("Jawa Barat");
        provinceJabar.setIsActive(true);

        provinceJatim = new Province();
        provinceJatim.setId(2L);
        provinceJatim.setName("Jawa Timur");
        provinceJatim.setIsActive(true);

        branchBandung = new Branch();
        branchBandung.setId(1L);
        branchBandung.setName("Cabang Bandung");
        branchBandung.setProvince(provinceJabar);
        branchBandung.setIsActive(true);

        branchSurabaya = new Branch();
        branchSurabaya.setId(2L);
        branchSurabaya.setName("Cabang Surabaya");
        branchSurabaya.setProvince(provinceJatim);
        branchSurabaya.setIsActive(true);

        regularStore = new Store();
        regularStore.setId(10L);
        regularStore.setName("Indomaret Dago");
        regularStore.setBranch(branchBandung);
        regularStore.setIsActive(true);

        whitelistStore = new Store();
        whitelistStore.setId(20L);
        whitelistStore.setName("Indomaret Tunjungan");
        whitelistStore.setBranch(branchSurabaya);
        whitelistStore.setIsActive(true);

        activeWhitelist = new WhitelistStore();
        activeWhitelist.setId(1L);
        activeWhitelist.setStore(whitelistStore);
        activeWhitelist.setIsActive(true);
    }

    @Test
    @DisplayName("Search Stores with Whitelist Priority: Whitelist store always prioritized on top")
    void testSearchStores_whitelistPriorityOnTop() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Store> regularPage = new PageImpl<>(List.of(regularStore), pageable, 1);

        when(storeRepository.searchByProvinceName(eq("Jawa Barat"), any(Pageable.class)))
                .thenReturn(regularPage);
        when(whitelistStoreRepository.findAllActiveWithDetails())
                .thenReturn(List.of(activeWhitelist));

        PagedResponse<StoreResponse> response = storeService.searchStores("Jawa Barat", pageable);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());

        // Whitelist store must be at index 0
        StoreResponse topStore = response.getContent().get(0);
        assertEquals(20L, topStore.getId());
        assertTrue(topStore.isWhitelisted());
        assertEquals("Indomaret Tunjungan", topStore.getName());

        // Regular search store must be at index 1
        StoreResponse secondStore = response.getContent().get(1);
        assertEquals(10L, secondStore.getId());
        assertFalse(secondStore.isWhitelisted());
        assertEquals("Indomaret Dago", secondStore.getName());
    }

    @Test
    @DisplayName("Search Stores: Whitelisted store in searched province is not duplicated")
    void testSearchStores_whitelistedStoreNotDuplicated() {
        Pageable pageable = PageRequest.of(0, 10);
        // Whitelist store is also matched by search criteria
        Page<Store> regularPage = new PageImpl<>(List.of(regularStore, whitelistStore), pageable, 2);

        when(storeRepository.searchByProvinceName(eq(""), any(Pageable.class)))
                .thenReturn(regularPage);
        when(whitelistStoreRepository.findAllActiveWithDetails())
                .thenReturn(List.of(activeWhitelist));

        PagedResponse<StoreResponse> response = storeService.searchStores("", pageable);

        assertNotNull(response);
        // Should have exactly 2 stores, not 3 (no duplicate)
        assertEquals(2, response.getContent().size());
        assertEquals(20L, response.getContent().get(0).getId());
        assertTrue(response.getContent().get(0).isWhitelisted());

        assertEquals(10L, response.getContent().get(1).getId());
        assertFalse(response.getContent().get(1).isWhitelisted());
    }

    @Test
    @DisplayName("Get Store by ID: returns store details with whitelist status")
    void testGetStoreById_success() {
        when(storeRepository.findActiveByIdWithBranchAndProvince(10L))
                .thenReturn(Optional.of(regularStore));
        when(whitelistStoreRepository.existsByStoreIdAndIsActiveTrue(10L))
                .thenReturn(false);

        StoreResponse response = storeService.getStoreById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Indomaret Dago", response.getName());
        assertEquals("Cabang Bandung", response.getBranchName());
        assertEquals("Jawa Barat", response.getProvinceName());
        assertFalse(response.isWhitelisted());
    }

    @Test
    @DisplayName("Get Store by ID: throws ResourceNotFoundException if not found")
    void testGetStoreById_notFound() {
        when(storeRepository.findActiveByIdWithBranchAndProvince(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> storeService.getStoreById(999L));
    }
}
