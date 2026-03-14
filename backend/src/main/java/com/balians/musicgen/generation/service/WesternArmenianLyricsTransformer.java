package com.balians.musicgen.generation.service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WesternArmenianLyricsTransformer {

    private static final Map<Character, Character> REPLACEMENTS = Map.ofEntries(
            Map.entry('բ', 'փ'),
            Map.entry('ձ', 'ց'),
            Map.entry('գ', 'ք'),
            Map.entry('ճ', 'ջ'),
            Map.entry('դ', 'թ'),
            Map.entry('պ', 'բ'),
            Map.entry('ծ', 'ձ'),
            Map.entry('ջ', 'չ'),
            Map.entry('կ', 'գ'),
            Map.entry('տ', 'դ'),
            Map.entry('Բ', 'Փ'),
            Map.entry('Ձ', 'Ց'),
            Map.entry('Գ', 'Ք'),
            Map.entry('Ճ', 'Ջ'),
            Map.entry('Դ', 'Թ'),
            Map.entry('Պ', 'Բ'),
            Map.entry('Ծ', 'Ձ'),
            Map.entry('Ջ', 'Չ'),
            Map.entry('Կ', 'Գ'),
            Map.entry('Տ', 'Դ')
    );

    public String transform(String lyrics) {
        if (lyrics == null || lyrics.isBlank()) {
            return lyrics;
        }

        StringBuilder result = new StringBuilder(lyrics.length());
        for (int index = 0; index < lyrics.length(); index++) {
            char current = lyrics.charAt(index);
            result.append(REPLACEMENTS.getOrDefault(current, current));
        }
        return result.toString();
    }
}
