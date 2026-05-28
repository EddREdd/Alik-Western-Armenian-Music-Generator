package com.balians.musicgen.admin.dto;

import jakarta.validation.constraints.NotNull;

public record SetReadyLibraryPublishedRequest(
        @NotNull(message = "published is required")
        Boolean published
) {
}
