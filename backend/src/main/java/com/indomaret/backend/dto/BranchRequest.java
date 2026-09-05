package com.indomaret.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BranchRequest {

    @NotBlank(message = "Nama cabang wajib diisi")
    private String name;

    private String address;

    @NotNull(message = "ID Provinsi wajib diisi")
    private Long provinceId;
}
