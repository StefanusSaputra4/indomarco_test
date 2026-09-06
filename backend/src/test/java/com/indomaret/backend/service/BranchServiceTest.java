package com.indomaret.backend.service;

import java.util.List;
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

import com.indomaret.backend.dto.BranchRequest;
import com.indomaret.backend.dto.BranchResponse;
import com.indomaret.backend.entity.Branch;
import com.indomaret.backend.entity.Province;
import com.indomaret.backend.entity.User;
import com.indomaret.backend.exception.ResourceNotFoundException;
import com.indomaret.backend.repository.BranchRepository;
import com.indomaret.backend.repository.ProvinceRepository;
import com.indomaret.backend.service.impl.BranchServiceImpl;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProvinceRepository provinceRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BranchServiceImpl branchService;

    private User currentUser;
    private Province province;
    private Branch branch;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin");

        province = new Province();
        province.setId(1L);
        province.setName("Jawa Barat");
        province.setIsActive(true);

        branch = new Branch();
        branch.setId(10L);
        branch.setName("Cabang Bandung");
        branch.setAddress("Jl. Soekarno Hatta No. 123");
        branch.setProvince(province);
        branch.setIsActive(true);
    }

    @Test
    @DisplayName("Create Branch: saves branch and logs CREATE audit")
    void testCreateBranch_success() {
        BranchRequest request = new BranchRequest();
        request.setName("Cabang Bandung");
        request.setAddress("Jl. Soekarno Hatta No. 123");
        request.setProvinceId(1L);

        when(provinceRepository.findById(1L)).thenReturn(Optional.of(province));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> {
            Branch b = invocation.getArgument(0);
            b.setId(10L);
            return b;
        });

        BranchResponse response = branchService.createBranch(request, currentUser);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Cabang Bandung", response.getName());

        verify(auditLogService).logChange(eq(currentUser), eq("Branch"), eq(10L), eq("CREATE"), eq(null), any());
    }

    @Test
    @DisplayName("Update Branch: updates branch details and logs UPDATE audit")
    void testUpdateBranch_success() {
        BranchRequest request = new BranchRequest();
        request.setName("Cabang Bandung Barat");
        request.setAddress("Jl. Sukajadi No. 50");
        request.setProvinceId(1L);

        when(branchRepository.findActiveByIdWithProvince(10L)).thenReturn(Optional.of(branch));
        when(provinceRepository.findById(1L)).thenReturn(Optional.of(province));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchResponse response = branchService.updateBranch(10L, request, currentUser);

        assertNotNull(response);
        assertEquals("Cabang Bandung Barat", response.getName());
        assertEquals("Jl. Sukajadi No. 50", response.getAddress());

        verify(auditLogService).logChange(eq(currentUser), eq("Branch"), eq(10L), eq("UPDATE"), any(), any());
    }

    @Test
    @DisplayName("Delete Branch: sets isActive=false, deletedAt timestamp, and logs DELETE audit")
    void testDeleteBranch_softDelete() {
        when(branchRepository.findActiveByIdWithProvince(10L)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        branchService.deleteBranch(10L, currentUser);

        assertFalse(branch.getIsActive());
        assertNotNull(branch.getDeletedAt());

        verify(branchRepository).save(branch);
        verify(auditLogService).logChange(eq(currentUser), eq("Branch"), eq(10L), eq("DELETE"), any(), eq(null));
    }

    @Test
    @DisplayName("Get Branch by ID: throws ResourceNotFoundException if branch not found or deleted")
    void testGetBranchById_notFound() {
        when(branchRepository.findActiveByIdWithProvince(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> branchService.getBranchById(999L));
    }

    @Test
    @DisplayName("Get All Active Branches: returns only non-deleted active branches")
    void testGetAllActiveBranches() {
        when(branchRepository.findAllByIsActiveTrueAndDeletedAtIsNull()).thenReturn(List.of(branch));

        List<BranchResponse> responses = branchService.getAllActiveBranches();

        assertEquals(1, responses.size());
        assertEquals("Cabang Bandung", responses.get(0).getName());
    }
}
