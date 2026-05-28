package com.balians.musicgen.provider.service;

import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.service.GenerationConstraints;
import com.balians.musicgen.provider.config.ProviderProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProviderSubmissionValidator {

    private final ProviderProperties providerProperties;

    public void validateForSubmission(GenerationJob job) {
        if (Boolean.TRUE.equals(job.getInstrumental())) {
            throw new BadRequestException("Instrumental generation is disabled.");
        }

        if (job.getModel() == null || !isSupportedModel(job.getModel())) {
            throw new BadRequestException("Only Suno v5.5 is supported.");
        }

        if (Boolean.FALSE.equals(job.getCustomMode())) {
            requireText(job.getPromptFinal(), "promptFinal is required when customMode is false");
            rejectIfPresent(job.getStyleFinal(), "styleFinal must be empty when customMode is false");
            rejectIfPresent(job.getTitleFinal(), "titleFinal must be empty when customMode is false");
            validatePromptLength(job.getPromptFinal(), job.getModel());
            return;
        }

        requireText(job.getStyleFinal(), "styleFinal is required when customMode is true");
        requireText(job.getTitleFinal(), "titleFinal is required when customMode is true");
        validateStyleLength(job.getStyleFinal(), job.getModel());
        validateTitleLength(job.getTitleFinal());

        requireText(job.getPromptFinal(), "promptFinal is required when customMode is true and instrumental is false");
        validatePromptLength(job.getPromptFinal(), job.getModel());
    }

    public String toProviderModel(GenerationModel model) {
        if (!isSupportedModel(model)) {
            throw new BadRequestException("Only Suno v5.5 is supported.");
        }
        return providerProperties.getSuno().getDefaultModel();
    }

    private boolean isSupportedModel(GenerationModel model) {
        return model == GenerationModel.V5_5 || model == GenerationModel.V5;
    }

    private void validatePromptLength(String prompt, GenerationModel model) {
        String trimmed = prompt.trim();
        if (trimmed.length() < GenerationConstraints.MIN_PROMPT_LENGTH) {
            throw new BadRequestException("Lyrics must be at least 50 characters.");
        }
        int max = model == GenerationModel.V3_5 || model == GenerationModel.V4 ? 3000 : 5000;
        if (trimmed.length() > max) {
            throw new BadRequestException("promptFinal exceeds provider limit of " + max + " characters");
        }
    }

    private void validateStyleLength(String style, GenerationModel model) {
        int max = model == GenerationModel.V3_5 || model == GenerationModel.V4 ? 200 : 1000;
        if (style.trim().length() > max) {
            throw new BadRequestException("styleFinal exceeds provider limit of " + max + " characters");
        }
    }

    private void validateTitleLength(String title) {
        int max = titleMaxLength();
        if (title.trim().length() > max) {
            throw new BadRequestException("titleFinal exceeds provider limit of " + max + " characters");
        }
    }

    private int titleMaxLength() {
        return 100;
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(message);
        }
    }

    private void rejectIfPresent(String value, String message) {
        if (value != null && !value.trim().isEmpty()) {
            throw new BadRequestException(message);
        }
    }
}
