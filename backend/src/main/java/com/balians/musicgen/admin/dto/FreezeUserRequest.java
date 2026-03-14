package com.balians.musicgen.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FreezeUserRequest(
        @NotBlank @Size(max = 250) String reason
) {
}
