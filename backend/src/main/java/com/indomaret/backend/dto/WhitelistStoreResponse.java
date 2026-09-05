package com.indomaret.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhitelistStoreResponse {

    private Long id;
    private Long storeId;
    private String storeName;
    private String branchName;
    private String provinceName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
