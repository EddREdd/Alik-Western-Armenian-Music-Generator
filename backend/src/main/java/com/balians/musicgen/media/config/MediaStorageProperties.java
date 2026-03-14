package com.balians.musicgen.media.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "media.storage")
public class MediaStorageProperties {

    private String rootPath = "storage";
    private String publicBaseUrl = "http://localhost:8080";
}
