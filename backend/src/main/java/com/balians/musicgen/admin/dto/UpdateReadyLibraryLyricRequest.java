package com.balians.musicgen.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReadyLibraryLyricRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @NotBlank(message = "body is required")
        @Size(max = 5000, message = "body must be at most 5000 characters")
        String body,

        @NotBlank(message = "language is required")
        @Size(max = 50, message = "language must be at most 50 characters")
        String language
) {
}
