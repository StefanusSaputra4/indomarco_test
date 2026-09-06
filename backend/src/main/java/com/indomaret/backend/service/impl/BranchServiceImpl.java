package com.indomaret.backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.indomaret.backend.dto.BranchRequest;
import com.indomaret.backend.dto.BranchResponse;
import com.indomaret.backend.entity.Branch;
import com.indomaret.backend.entity.Province;
import com.indomaret.backend.entity.User;
import com.indomaret.backend.exception.ResourceNotFoundException;
import com.indomaret.backend.repository.BranchRepository;
import com.indomaret.backend.repository.ProvinceRepository;
import com.indomaret.backend.service.AuditLogService;
import com.indomaret.backend.service.BranchService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final ProvinceRepository provinceRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public BranchResponse createBranch(BranchRequest request, User currentUser) {
        Province province = provinceRepository.findById(request.getProvinceId())
                .filter(p -> p.getIsActive() && p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Provinsi tidak ditemukan dengan ID: " + request.getProvinceId()));

        Branch branch = new Branch();
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setProvince(province);
        branch.setIsActive(true);

        Branch savedBranch = branchRepository.save(branch);
        auditLogService.logChange(currentUser, "Branch", savedBranch.getId(), "CREATE", null, mapToResponse(savedBranch));

        return mapToResponse(savedBranch);
    }

    @Override
    @Transactional
    public BranchResponse updateBranch(Long id, BranchRequest request, User currentUser) {
        Branch branch = branchRepository.findActiveByIdWithProvince(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cabang tidak ditemukan dengan ID: " + id));

        Province province = provinceRepository.findById(request.getProvinceId())
                .filter(p -> p.getIsActive() && p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Provinsi tidak ditemukan dengan ID: " + request.getProvinceId()));

        BranchResponse oldState = mapToResponse(branch);

        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setProvince(province);

        Branch updatedBranch = branchRepository.save(branch);
        BranchResponse newState = mapToResponse(updatedBranch);

        auditLogService.logChange(currentUser, "Branch", updatedBranch.getId(), "UPDATE", oldState, newState);

        return newState;
    }

    @Override
    @Transactional
    public void deleteBranch(Long id, User currentUser) {
        Branch branch = branchRepository.findActiveByIdWithProvince(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cabang tidak ditemukan dengan ID: " + id));

        BranchResponse oldState = mapToResponse(branch);

        branch.setIsActive(false);
        branch.setDeletedAt(LocalDateTime.now());
        branchRepository.save(branch);

        auditLogService.logChange(currentUser, "Branch", branch.getId(), "DELETE", oldState, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchById(Long id) {
        return branchRepository.findActiveByIdWithProvince(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Cabang tidak ditemukan dengan ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllActiveBranches() {
        return branchRepository.findAllByIsActiveTrueAndDeletedAtIsNull()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BranchResponse mapToResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .provinceId(branch.getProvince() != null ? branch.getProvince().getId() : null)
                .provinceName(branch.getProvince() != null ? branch.getProvince().getName() : null)
                .isActive(branch.getIsActive())
                .createdAt(branch.getCreatedAt())
                .build();
    }
}
