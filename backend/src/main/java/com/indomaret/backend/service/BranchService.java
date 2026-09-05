package com.indomaret.backend.service;

import java.util.List;

import com.indomaret.backend.dto.BranchRequest;
import com.indomaret.backend.dto.BranchResponse;
import com.indomaret.backend.entity.User;

public interface BranchService {

    BranchResponse createBranch(BranchRequest request, User currentUser);

    BranchResponse updateBranch(Long id, BranchRequest request, User currentUser);

    void deleteBranch(Long id, User currentUser);

    BranchResponse getBranchById(Long id);

    List<BranchResponse> getAllActiveBranches();
}
