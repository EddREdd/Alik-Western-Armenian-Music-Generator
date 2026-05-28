package com.balians.musicgen.lyrics.service;

import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.lyrics.model.LyricLanguage;
import org.springframework.stereotype.Component;

@Component
public class LyricTextValidator {

    public void validateTitle(String title, LyricLanguage language) {
        validateText(title, "title", language, false);
    }

    public void validateBody(String body, LyricLanguage language) {
        validateText(body, "body", language, true);
    }

    private void validateText(String value, String fieldName, LyricLanguage language, boolean requireScriptLetter) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }

        if (!requireScriptLetter) {
            return;
        }

        boolean hasRequiredLetter = switch (language) {
            case WESTERN_ARMENIAN -> containsArmenianLetter(trimmed);
            case ENGLISH -> containsLatinLetter(trimmed);
        };

        if (!hasRequiredLetter) {
            throw new BadRequestException(switch (language) {
                case WESTERN_ARMENIAN -> fieldName + " must contain at least one Armenian letter";
                case ENGLISH -> fieldName + " must contain at least one Latin letter";
            });
        }
    }

    private boolean containsArmenianLetter(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.UnicodeBlock.of(value.charAt(index)) == Character.UnicodeBlock.ARMENIAN
                    && Character.isLetter(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLatinLetter(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isLetter(current) && isBasicLatinLetter(current)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBasicLatinLetter(char current) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(current);
        return block == Character.UnicodeBlock.BASIC_LATIN
                || block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT
                || block == Character.UnicodeBlock.LATIN_EXTENDED_A
                || block == Character.UnicodeBlock.LATIN_EXTENDED_B;
    }
}
