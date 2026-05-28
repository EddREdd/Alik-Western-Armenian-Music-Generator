package com.balians.musicgen.provider.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderSunoProperties {

    /**
     * Model string sent to Suno API (e.g. V5_5 per https://docs.sunoapi.org/suno-api/generate-music).
     */
    private String defaultModel = "V5_5";
}
