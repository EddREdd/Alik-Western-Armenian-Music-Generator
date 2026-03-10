package com.balians.musicgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableMongoAuditing
@ConfigurationPropertiesScan
@SpringBootApplication
public class MusicgenBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicgenBackendApplication.class, args);
    }
}
