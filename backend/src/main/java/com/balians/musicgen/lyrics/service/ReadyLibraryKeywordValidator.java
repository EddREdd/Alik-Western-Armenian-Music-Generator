package com.balians.musicgen.lyrics.service;

import com.balians.musicgen.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class ReadyLibraryKeywordValidator {

    public static final int MAX_KEYWORD_LENGTH = 100;

    public void validate(String keyword) {
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new BadRequestException("Search keyword must be 100 characters or fewer.");
        }
    }

    public String normalize(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        validate(trimmed);
        return trimmed;
    }
}
