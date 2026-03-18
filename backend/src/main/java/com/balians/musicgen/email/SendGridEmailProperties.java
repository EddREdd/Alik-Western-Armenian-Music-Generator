package com.balians.musicgen.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "email.sendgrid")
public class SendGridEmailProperties {

    /**
     * SendGrid API key. Must be provided via SENDGRID_API_KEY env var in real environments.
     */
    private String apiKey;

    /**
     * Default from email address used for outgoing messages.
     */
    private String fromAddress = "no-reply@example.com";

    /**
     * Default from name used for outgoing messages.
     */
    private String fromName = "Alik Music";
}

