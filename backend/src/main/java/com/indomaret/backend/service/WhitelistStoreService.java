package com.indomaret.backend.service;

import java.util.List;

import com.indomaret.backend.dto.WhitelistStoreRequest;
import com.indomaret.backend.dto.WhitelistStoreResponse;
import com.indomaret.backend.entity.User;

public interface WhitelistStoreService {

    WhitelistStoreResponse addStoreToWhitelist(WhitelistStoreRequest request, User currentUser);

    WhitelistStoreResponse toggleWhitelistStatus(Long id, boolean isActive, User currentUser);

    void removeStoreFromWhitelist(Long id, User currentUser);

    List<WhitelistStoreResponse> getAllActiveWhitelistStores();
}
