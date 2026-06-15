package com.balians.musicgen.email;

import com.balians.musicgen.email.template.EmailTemplateService;
import com.balians.musicgen.email.template.EmailTemplateType;
import com.balians.musicgen.email.template.RenderedEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendTemplateTestEmailRequest(
        @Email(message = "to must be a valid email")
        @NotBlank(message = "to is required")
        String to,

        @NotNull(message = "template is required")
        EmailTemplateType template,

        String inviteCode,
        String otpCode
) {
}
