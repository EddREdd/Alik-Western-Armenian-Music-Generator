package com.balians.musicgen.generation.dto;

import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.JobSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGenerationJobRequest(
        @NotBlank(message = "projectId is required")
        @Size(max = 100, message = "projectId must be at most 100 characters")
        String projectId,

        @Size(max = 100, message = "templateId must be at most 100 characters")
        String templateId,

        @Size(max = 100, message = "lyricId must be at most 100 characters")
        String lyricId,

        @NotNull(message = "sourceType is required")
        JobSourceType sourceType,

        @NotBlank(message = "promptFinal is required")
        @Size(max = 2000, message = "promptFinal must be at most 2000 characters")
        String promptFinal,

        @Size(max = 500, message = "styleFinal must be at most 500 characters")
        String styleFinal,

        @Size(max = 255, message = "titleFinal must be at most 255 characters")
        String titleFinal,

        @NotNull(message = "customMode is required")
        Boolean customMode,

        @NotNull(message = "instrumental is required")
        Boolean instrumental,

        @NotNull(message = "model is required")
        GenerationModel model
) {
}
