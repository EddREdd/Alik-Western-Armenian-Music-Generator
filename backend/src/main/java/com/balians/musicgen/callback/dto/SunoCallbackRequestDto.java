package com.balians.musicgen.callback.dto;

public record SunoCallbackRequestDto(
        Integer code,
        String msg,
        SunoCallbackDataDto data
) {
}
