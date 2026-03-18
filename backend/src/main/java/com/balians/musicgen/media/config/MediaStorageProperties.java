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

    /**
     * Storage type: "local" (default) or "spaces".
     */
    private String type = "local";

    /**
     * DigitalOcean Spaces configuration (S3 compatible).
     * These are only used when type == "spaces".
     */
    private String spacesEndpoint;
    private String spacesRegion;
    private String spacesBucket;
    private String spacesAccessKey;
    private String spacesSecretKey;
    /**
     * Base public URL for files in the bucket, e.g.
     * https://your-space.nyc3.cdn.digitaloceanspaces.com
     */
    private String spacesPublicBaseUrl;
}
