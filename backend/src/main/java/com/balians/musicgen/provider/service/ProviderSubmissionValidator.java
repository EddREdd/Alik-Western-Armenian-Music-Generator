package com.balians.musicgen.provider.service;

import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.generation.model.GenerationJob;
import org.springframework.stereotype.Component;

@Component
public class ProviderSubmissionValidator {

    public void validateForSubmission(GenerationJob job) {
        if (job.getModel() == null || !isSupportedModel(job.getModel())) {
            throw new BadRequestException("Unsupported provider model");
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

        if (Boolean.FALSE.equals(job.getInstrumental())) {
            requireText(job.getPromptFinal(), "promptFinal is required when customMode is true and instrumental is false");
            validatePromptLength(job.getPromptFinal(), job.getModel());
        }
    }

    public String toProviderModel(GenerationModel model) {
        if (!isSupportedModel(model)) {
            throw new BadRequestException("Unsupported provider model");
        }
        return model.name();
    }

    private boolean isSupportedModel(GenerationModel model) {
        return model == GenerationModel.V5;
    }

    private void validatePromptLength(String prompt, GenerationModel model) {
        int max = model == GenerationModel.V3_5 || model == GenerationModel.V4 ? 3000 : 5000;
        if (prompt.trim().length() > max) {
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
        if (title.trim().length() > 80) {
            throw new BadRequestException("titleFinal exceeds provider limit of 80 characters");
        }
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
