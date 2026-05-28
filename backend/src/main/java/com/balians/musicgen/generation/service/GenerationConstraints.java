package com.balians.musicgen.generation.service;

import com.balians.musicgen.common.enums.GenerationModel;

public final class GenerationConstraints {

    public static final GenerationModel REQUIRED_MODEL = GenerationModel.V5_5;
    public static final int MIN_PROMPT_LENGTH = 50;

    private GenerationConstraints() {
    }
}
