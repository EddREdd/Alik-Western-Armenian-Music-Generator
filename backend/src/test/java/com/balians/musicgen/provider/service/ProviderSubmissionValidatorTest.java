package com.balians.musicgen.provider.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.provider.config.ProviderProperties;
import com.balians.musicgen.provider.config.ProviderSunoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProviderSubmissionValidatorTest {

    private ProviderSubmissionValidator validator;

    @BeforeEach
    void setUp() {
        ProviderProperties properties = new ProviderProperties();
        properties.setBaseUrl("https://api.sunoapi.org");
        properties.setApiKey("test-key");
        properties.setCallbackBaseUrl("http://localhost:8080");
        ProviderSunoProperties suno = new ProviderSunoProperties();
        suno.setDefaultModel("V5_5");
        properties.setSuno(suno);
        validator = new ProviderSubmissionValidator(properties);
    }

    @Test
    void toProviderModel_v5_5_usesConfiguredSunoDefault() {
        assertThat(validator.toProviderModel(GenerationModel.V5_5)).isEqualTo("V5_5");
    }

    @Test
    void toProviderModel_legacyV5_usesConfiguredSunoDefault() {
        assertThat(validator.toProviderModel(GenerationModel.V5)).isEqualTo("V5_5");
    }

    @Test
    void toProviderModel_unsupportedModel_isRejected() {
        assertThatThrownBy(() -> validator.toProviderModel(GenerationModel.V4))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only Suno v5.5 is supported.");
    }

    @Test
    void validateForSubmission_instrumentalTrue_isRejected() {
        GenerationJob job = baseJob();
        job.setInstrumental(true);

        assertThatThrownBy(() -> validator.validateForSubmission(job))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Instrumental generation is disabled.");
    }

    @Test
    void validateForSubmission_promptUnder50Characters_isRejected() {
        GenerationJob job = baseJob();
        job.setPromptFinal("too short");

        assertThatThrownBy(() -> validator.validateForSubmission(job))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lyrics must be at least 50 characters.");
    }

    @Test
    void validateForSubmission_promptWith50Characters_isAccepted() {
        GenerationJob job = baseJob();
        job.setPromptFinal("a".repeat(50));

        validator.validateForSubmission(job);
    }

    @Test
    void validateForSubmission_legacyV5Job_isAccepted() {
        GenerationJob job = baseJob();
        job.setModel(GenerationModel.V5);
        job.setPromptFinal("a".repeat(50));

        validator.validateForSubmission(job);
    }

    private static GenerationJob baseJob() {
        return GenerationJob.builder()
                .id("job-1")
                .projectId("project-1")
                .sourceType(JobSourceType.MANUAL)
                .internalStatus(InternalJobStatus.VALIDATED)
                .providerStatus(ProviderJobStatus.NOT_SUBMITTED)
                .promptFinal("a".repeat(50))
                .styleFinal("style")
                .titleFinal("title")
                .customMode(true)
                .instrumental(false)
                .model(GenerationModel.V5_5)
                .build();
    }
}
