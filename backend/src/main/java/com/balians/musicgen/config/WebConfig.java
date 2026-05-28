package com.balians.musicgen.config;

import com.balians.musicgen.media.config.MediaStorageProperties;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private static final String[] CORS_PATH_PATTERNS = {
        "/api/**",
        "/media/**",
        "/actuator/**",
        "/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    private final CorsProperties corsProperties;
    private final MediaStorageProperties mediaStorageProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = corsProperties.getAllowedOrigins().stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .toArray(String[]::new);
        String[] allowedOriginPatterns = corsProperties.getAllowedOriginPatterns().stream()
                .filter(pattern -> pattern != null && !pattern.isBlank())
                .toArray(String[]::new);

        if (allowedOrigins.length == 0 && allowedOriginPatterns.length == 0) {
            return;
        }

        for (String pathPattern : CORS_PATH_PATTERNS) {
            var mapping = registry.addMapping(pathPattern)
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders(
                            "Content-Disposition",
                            HttpHeaders.ACCEPT_RANGES,
                            HttpHeaders.CONTENT_RANGE,
                            HttpHeaders.CONTENT_LENGTH
                    )
                    .allowCredentials(false);

            if (allowedOrigins.length > 0) {
                mapping.allowedOrigins(allowedOrigins);
            }
            if (allowedOriginPatterns.length > 0) {
                mapping.allowedOriginPatterns(allowedOriginPatterns);
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path storageRoot = Path.of(mediaStorageProperties.getRootPath()).toAbsolutePath().normalize();
        registry.addResourceHandler("/media/**")
                .addResourceLocations(storageRoot.toUri().toString());
    }
}
