package com.balians.musicgen.email;

import com.balians.musicgen.common.response.StandardSuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email")
public class EmailTestController {

    private final SendGridEmailService sendGridEmailService;

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
}

