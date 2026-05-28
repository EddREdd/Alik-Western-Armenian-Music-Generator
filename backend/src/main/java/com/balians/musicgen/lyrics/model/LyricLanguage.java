package com.balians.musicgen.lyrics.model;

import com.balians.musicgen.common.exception.BadRequestException;

public enum LyricLanguage {
    ENGLISH,
    WESTERN_ARMENIAN;

    public static LyricLanguage parseRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            return LyricLanguage.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(fieldName + " must be ENGLISH or WESTERN_ARMENIAN");
        }
    }

    public static LyricLanguage parseOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseRequired(value, "language");
    }
}
