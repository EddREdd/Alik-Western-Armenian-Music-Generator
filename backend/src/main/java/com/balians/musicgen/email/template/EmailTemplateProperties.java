package com.balians.musicgen.email.template;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "email.templates")
public class EmailTemplateProperties {

    /**
     * Public app URL used in email CTAs (register, sign in, reset password).
     */
    private String publicAppUrl = "http://localhost:3000";

    private String supportEmail = "edward@alikfoundation.com";
}
