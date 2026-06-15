package com.balians.musicgen.email.template;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateProperties properties;

    private String layoutTemplate;
    private final Map<EmailTemplateType, String> bodyTemplates = new EnumMap<>(EmailTemplateType.class);

    @PostConstruct
    void loadTemplates() throws IOException {
        layoutTemplate = readClasspath("email/layout.html");
        bodyTemplates.put(EmailTemplateType.WELCOME, readClasspath("email/welcome.html"));
        bodyTemplates.put(EmailTemplateType.PASSWORD_RESET, readClasspath("email/password-reset.html"));
        bodyTemplates.put(EmailTemplateType.INVITE_CODE, readClasspath("email/invite-code.html"));
    }

    public RenderedEmail renderWelcomeEmail() {
        Map<String, String> values = baseValues();
        values.put("appUrl", properties.getPublicAppUrl());

        return render(
                EmailTemplateType.WELCOME,
                values,
                """
                        Welcome to Alik

                        Thank you for joining the Alik pilot program.

                        Open Alik: %s

                        We are glad to have you as part of the Alik community.
                        """.formatted(properties.getPublicAppUrl()).trim()
        );
    }

    public RenderedEmail renderPasswordResetEmail(String otpCode) {
        Map<String, String> values = baseValues();
        values.put("otpCode", escapeHtml(otpCode));
        values.put("appUrl", escapeHtml(properties.getPublicAppUrl()));

        return render(
                EmailTemplateType.PASSWORD_RESET,
                values,
                """
                        Reset your Alik password

                        Use this 5-digit code: %s

                        If you did not request this, you can ignore this email.

                        %s
                        """.formatted(otpCode, properties.getPublicAppUrl()).trim()
        );
    }

    public RenderedEmail renderInviteCodeEmail(String inviteCode) {
        Map<String, String> values = baseValues();
        values.put("inviteCode", escapeHtml(inviteCode));
        values.put("registerUrl", escapeHtml(registerUrl()));

        return render(
                EmailTemplateType.INVITE_CODE,
                values,
                """
                        You're invited to Alik

                        Use this invite code when registering: %s

                        Create your account: %s
                        """.formatted(inviteCode, registerUrl()).trim()
        );
    }

    private RenderedEmail render(EmailTemplateType type, Map<String, String> values, String plainTextBody) {
        String bodyTemplate = bodyTemplates.get(type);
        if (bodyTemplate == null) {
            throw new IllegalArgumentException("Missing email body template for " + type);
        }

        String content = applyValues(bodyTemplate, values);
        Map<String, String> layoutValues = new HashMap<>(values);
        layoutValues.put("content", content);
        layoutValues.put("subject", escapeHtml(type.defaultSubject()));

        String htmlBody = applyValues(layoutTemplate, layoutValues);
        return new RenderedEmail(type.defaultSubject(), htmlBody, plainTextBody);
    }

    private Map<String, String> baseValues() {
        Map<String, String> values = new HashMap<>();
        values.put("supportEmail", escapeHtml(properties.getSupportEmail()));
        return values;
    }

    private String registerUrl() {
        String base = properties.getPublicAppUrl().replaceAll("/+$", "");
        return base + "/?view=register";
    }

    private String applyValues(String template, Map<String, String> values) {
        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String readClasspath(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
