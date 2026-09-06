package com.indomaret.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {

    private Long id;
    private String name;
    private String address;
    private Long provinceId;
    private String provinceName;
    private Boolean isActive;
    private java.time.LocalDateTime createdAt;
}

