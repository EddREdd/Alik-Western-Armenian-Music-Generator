package com.balians.musicgen.email;

import com.balians.musicgen.common.response.StandardSuccessResponse;
import com.balians.musicgen.email.template.EmailTemplateService;
import com.balians.musicgen.email.template.EmailTemplateType;
import com.balians.musicgen.email.template.RenderedEmail;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email")
public class EmailTestController {

    private final SendGridEmailService sendGridEmailService;
    private final EmailTemplateService emailTemplateService;

    @PostMapping("/test")
    public StandardSuccessResponse<String> sendTestEmail(@Valid @RequestBody SendTestEmailRequest request) {
        try {
            boolean sent = sendGridEmailService.sendTextEmail(request.to(), request.subject(), request.body());
            return StandardSuccessResponse.ok(sent ? "queued" : "failed");
        } catch (Exception ex) {
            log.warn("Unexpected error while sending test email to={}", request.to(), ex);
            return StandardSuccessResponse.ok("failed: " + ex.getMessage());
        }
    }

    @PostMapping("/test-template")
    public StandardSuccessResponse<String> sendTemplateTestEmail(
            @Valid @RequestBody SendTemplateTestEmailRequest request
    ) {
        try {
            RenderedEmail rendered = renderTemplate(
                    request.template(),
                    request.inviteCode(),
                    request.otpCode()
            );
            boolean sent = sendGridEmailService.sendRenderedEmail(request.to(), rendered);
            return StandardSuccessResponse.ok(sent ? "queued" : "failed");
        } catch (Exception ex) {
            log.warn("Unexpected error while sending template test email to={}", request.to(), ex);
            return StandardSuccessResponse.ok("failed: " + ex.getMessage());
        }
    }

    @GetMapping("/templates/{template}/preview")
    public StandardSuccessResponse<RenderedEmail> previewTemplate(
            @PathVariable EmailTemplateType template,
            @RequestParam(required = false) String inviteCode,
            @RequestParam(required = false) String otpCode
    ) {
        return StandardSuccessResponse.ok(renderTemplate(template, inviteCode, otpCode));
    }

    private RenderedEmail renderTemplate(EmailTemplateType template, String inviteCode, String otpCode) {
        return switch (template) {
            case WELCOME -> emailTemplateService.renderWelcomeEmail();
            case PASSWORD_RESET -> emailTemplateService.renderPasswordResetEmail(
                    otpCode == null || otpCode.isBlank() ? "12345" : otpCode
            );
            case INVITE_CODE -> emailTemplateService.renderInviteCodeEmail(
                    inviteCode == null || inviteCode.isBlank() ? "ALIK-INVITE" : inviteCode
            );
        };
    }
}

