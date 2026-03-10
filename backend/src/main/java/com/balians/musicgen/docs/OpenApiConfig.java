package com.balians.musicgen.docs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI musicgenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Balians Music Generation Backend API")
                        .version("v1")
                        .description("""
                                Async music-generation backend integrating with Suno.

                                Important workflow note:
                                - submitting a generation job returns a provider taskId, not final music
                                - final completion is driven by provider callback and/or polling reconciliation
                                """)
                        .contact(new Contact().name("Balians Backend"))
                        .license(new License().name("Internal MVP")))
                .servers(List.of(new Server().url("/")));
    }
}
