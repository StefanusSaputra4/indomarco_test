package com.indomaret.backend.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.indomaret.backend.config.AppProperties;
import com.indomaret.backend.dto.WhitelistStoreRequest;
import com.indomaret.backend.dto.WhitelistStoreResponse;
import com.indomaret.backend.entity.Branch;
import com.indomaret.backend.entity.Province;
import com.indomaret.backend.entity.Store;
import com.indomaret.backend.entity.User;
import com.indomaret.backend.entity.WhitelistStore;
import com.indomaret.backend.exception.BadRequestException;
import com.indomaret.backend.repository.StoreRepository;
import com.indomaret.backend.repository.WhitelistStoreRepository;
import com.indomaret.backend.service.impl.WhitelistStoreServiceImpl;

@ExtendWith(MockitoExtension.class)
class WhitelistStoreServiceTest {

    @Mock
    private WhitelistStoreRepository whitelistStoreRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WhitelistStoreServiceImpl whitelistStoreService;

    private User currentUser;
    private Store store;
    private WhitelistStore whitelistStore;
    private AppProperties.Whitelist whitelistConfig;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin");

        Province province = new Province();
        province.setId(1L);
        province.setName("Jawa Barat");

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Cabang Bandung");
        branch.setProvince(province);

        store = new Store();
        store.setId(10L);
        store.setName("Indomaret Dago");
        store.setBranch(branch);
        store.setIsActive(true);

        whitelistStore = new WhitelistStore();
        whitelistStore.setId(1L);
        whitelistStore.setStore(store);
        whitelistStore.setIsActive(true);

        whitelistConfig = new AppProperties.Whitelist();
        whitelistConfig.setMaxStoreCount(50);
    }

    @Test
    @DisplayName("Add Store to Whitelist: success and logs CREATE audit")
    void testAddStoreToWhitelist_success() {
        WhitelistStoreRequest request = new WhitelistStoreRequest();
        request.setStoreId(10L);

        when(appProperties.getWhitelist()).thenReturn(whitelistConfig);
        when(whitelistStoreRepository.countByIsActiveTrue()).thenReturn(5L);
        when(storeRepository.findActiveByIdWithBranchAndProvince(10L)).thenReturn(Optional.of(store));
        when(whitelistStoreRepository.existsByStoreIdAndIsActiveTrue(10L)).thenReturn(false);
        when(whitelistStoreRepository.save(any(WhitelistStore.class))).thenAnswer(invocation -> {
            WhitelistStore ws = invocation.getArgument(0);
            ws.setId(1L);
            return ws;
        });

        WhitelistStoreResponse response = whitelistStoreService.addStoreToWhitelist(request, currentUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getStoreId());
        assertEquals("Indomaret Dago", response.getStoreName());

        verify(auditLogService).logChange(eq(currentUser), eq("WhitelistStore"), eq(1L), eq("CREATE"), eq(null), any());
    }

    @Test
    @DisplayName("Add Store to Whitelist: throws BadRequestException when quota limit is reached")
    void testAddStoreToWhitelist_quotaExceeded() {
        WhitelistStoreRequest request = new WhitelistStoreRequest();
        request.setStoreId(10L);

        when(appProperties.getWhitelist()).thenReturn(whitelistConfig);
        when(whitelistStoreRepository.countByIsActiveTrue()).thenReturn(50L); // Max is 50

        assertThrows(BadRequestException.class, () -> whitelistStoreService.addStoreToWhitelist(request, currentUser));
    }

    @Test
    @DisplayName("Add Store to Whitelist: throws BadRequestException when store is already whitelisted")
    void testAddStoreToWhitelist_duplicate() {
        WhitelistStoreRequest request = new WhitelistStoreRequest();
        request.setStoreId(10L);

        when(appProperties.getWhitelist()).thenReturn(whitelistConfig);
        when(whitelistStoreRepository.countByIsActiveTrue()).thenReturn(5L);
        when(storeRepository.findActiveByIdWithBranchAndProvince(10L)).thenReturn(Optional.of(store));
        when(whitelistStoreRepository.existsByStoreIdAndIsActiveTrue(10L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> whitelistStoreService.addStoreToWhitelist(request, currentUser));
    }

    @Test
    @DisplayName("Toggle Whitelist Status: toggles active status and logs UPDATE audit")
    void testToggleWhitelistStatus_success() {
        when(whitelistStoreRepository.findById(1L)).thenReturn(Optional.of(whitelistStore));
        when(whitelistStoreRepository.save(any(WhitelistStore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WhitelistStoreResponse response = whitelistStoreService.toggleWhitelistStatus(1L, false, currentUser);

        assertNotNull(response);
        assertFalse(response.getIsActive());

        verify(auditLogService).logChange(eq(currentUser), eq("WhitelistStore"), eq(1L), eq("UPDATE"), any(), any());
    }

    @Test
    @DisplayName("Remove Store from Whitelist: deletes entity and logs DELETE audit")
    void testRemoveStoreFromWhitelist_success() {
        when(whitelistStoreRepository.findById(1L)).thenReturn(Optional.of(whitelistStore));

        whitelistStoreService.removeStoreFromWhitelist(1L, currentUser);

        verify(whitelistStoreRepository).delete(whitelistStore);
        verify(auditLogService).logChange(eq(currentUser), eq("WhitelistStore"), eq(1L), eq("DELETE"), any(), eq(null));
    }
}
