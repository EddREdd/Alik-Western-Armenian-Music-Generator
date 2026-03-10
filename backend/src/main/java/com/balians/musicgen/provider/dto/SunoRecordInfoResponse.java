package com.balians.musicgen.provider.dto;

public record SunoRecordInfoResponse(
        Integer code,
        String msg,
        SunoRecordInfoDataDto data
) {
}
