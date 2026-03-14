package com.balians.musicgen.generation.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WesternArmenianLyricsTransformerTest {

    private final WesternArmenianLyricsTransformer transformer = new WesternArmenianLyricsTransformer();

    @Test
    void transform_replacesConfiguredWesternArmenianCharacters() {
        assertThat(transformer.transform("բձգճդպծջկտ ԲՁԳՃԴՊԾՋԿՏ"))
                .isEqualTo("փցքջթբձչգդ ՓՑՔՋԹԲՁՉԳԴ");
    }

    @Test
    void transform_keepsOtherCharactersUntouched() {
        assertThat(transformer.transform("Արամ style 123"))
                .isEqualTo("Արամ style 123");
    }
}
