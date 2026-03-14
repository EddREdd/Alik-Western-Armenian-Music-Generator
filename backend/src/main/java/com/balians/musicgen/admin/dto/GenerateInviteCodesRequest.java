package com.balians.musicgen.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateInviteCodesRequest(
        @Min(1) @Max(100) int count
) {
}
