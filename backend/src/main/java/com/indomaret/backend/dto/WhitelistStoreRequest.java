package com.indomaret.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WhitelistStoreRequest {

    @NotNull(message = "ID Toko wajib diisi")
    private Long storeId;
}
