package com.balians.musicgen.lyrics.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.lyrics.model.LyricLanguage;
import org.junit.jupiter.api.Test;

class LyricTextValidatorTest {

    private final LyricTextValidator validator = new LyricTextValidator();

    @Test
    void englishValidationPasses() {
        assertThatCode(() -> validator.validateBody("[Verse] Hello world 123", LyricLanguage.ENGLISH))
                .doesNotThrowAnyException();
    }

    @Test
    void westernArmenianValidationPasses() {
        assertThatCode(() -> validator.validateBody("[Chorus] \u0561\u0562\u0563 \u0564\u0565\u0566", LyricLanguage.WESTERN_ARMENIAN))
                .doesNotThrowAnyException();
    }

    @Test
    void blankOrNoLetterFails() {
        assertThatThrownBy(() -> validator.validateBody("   ", LyricLanguage.ENGLISH))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validateBody("1234 []!!", LyricLanguage.WESTERN_ARMENIAN))
                .isInstanceOf(BadRequestException.class);
    }
}