package com.indomaret.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {

    private Long id;
    private String name;
    private String address;
    private Long branchId;
    private String branchName;
    private Long provinceId;
    private String provinceName;
    private java.time.LocalDateTime createdAt;
    
    @Builder.Default
    private boolean whitelisted = false;
}

