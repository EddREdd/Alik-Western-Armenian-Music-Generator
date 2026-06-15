package com.balians.musicgen.email.template;

public record RenderedEmail(
        String subject,
        String htmlBody,
        String plainTextBody
) {
}
