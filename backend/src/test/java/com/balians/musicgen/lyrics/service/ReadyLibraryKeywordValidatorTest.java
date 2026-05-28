package com.balians.musicgen.lyrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.balians.musicgen.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class ReadyLibraryKeywordValidatorTest {

    private final ReadyLibraryKeywordValidator validator = new ReadyLibraryKeywordValidator();

    @Test
    void validate_acceptsKeywordWithinLimit() {
        validator.validate("a".repeat(100));
    }

    @Test
    void validate_rejectsKeywordOverLimit() {
        assertThatThrownBy(() -> validator.validate("a".repeat(101)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("100 characters or fewer");
    }

    @Test
    void normalize_returnsNullForBlank() {
        assertThat(validator.normalize("   ")).isNull();
    }

    @Test
    void normalize_trimsKeyword() {
        assertThat(validator.normalize("  hello  ")).isEqualTo("hello");
    }
}
