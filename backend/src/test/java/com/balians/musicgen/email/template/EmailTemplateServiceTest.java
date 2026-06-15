package com.balians.musicgen.email.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() throws Exception {
        EmailTemplateProperties properties = new EmailTemplateProperties();
        properties.setPublicAppUrl("https://alik.example.com");
        properties.setSupportEmail("support@alik.example.com");

        emailTemplateService = new EmailTemplateService(properties);
        emailTemplateService.loadTemplates();
    }

    @Test
    void renderWelcomeEmail_usesBrandedHtmlLayout() {
        RenderedEmail rendered = emailTemplateService.renderWelcomeEmail();

        assertEquals("Welcome to Alik", rendered.subject());
        assertTrue(rendered.htmlBody().contains("background-color:#004144"));
        assertTrue(rendered.htmlBody().contains("Western Armenian Music Generator"));
        assertTrue(rendered.htmlBody().contains("https://alik.example.com"));
        assertTrue(rendered.plainTextBody().contains("Welcome to Alik"));
    }

    @Test
    void renderPasswordResetEmail_includesOtpCode() {
        RenderedEmail rendered = emailTemplateService.renderPasswordResetEmail("12345");

        assertEquals("Your Alik password reset code", rendered.subject());
        assertTrue(rendered.htmlBody().contains("12345"));
        assertTrue(rendered.htmlBody().contains("Reset your password"));
        assertTrue(rendered.plainTextBody().contains("12345"));
    }

    @Test
    void renderInviteCodeEmail_includesInviteCodeAndRegisterLink() {
        RenderedEmail rendered = emailTemplateService.renderInviteCodeEmail("ALIK-PILOT-01");

        assertEquals("You're invited to Alik", rendered.subject());
        assertTrue(rendered.htmlBody().contains("ALIK-PILOT-01"));
        assertTrue(rendered.htmlBody().contains("https://alik.example.com/?view=register"));
        assertTrue(rendered.plainTextBody().contains("ALIK-PILOT-01"));
    }
}
