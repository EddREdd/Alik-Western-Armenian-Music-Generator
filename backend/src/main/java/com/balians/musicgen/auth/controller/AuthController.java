package com.balians.musicgen.auth.controller;

import com.balians.musicgen.auth.dto.AuthSessionResponse;
import com.balians.musicgen.auth.dto.AuthUserResponse;
import com.balians.musicgen.auth.dto.ChangeEmailRequest;
import com.balians.musicgen.auth.dto.ChangePasswordRequest;
import com.balians.musicgen.auth.dto.ForgotPasswordRequest;
import com.balians.musicgen.auth.dto.ForgotPasswordResetRequest;
import com.balians.musicgen.auth.dto.ForgotPasswordVerifyRequest;
import com.balians.musicgen.auth.dto.GoogleAuthRequest;
import com.balians.musicgen.auth.dto.LoginRequest;
import com.balians.musicgen.auth.dto.OtpChallengeResponse;
import com.balians.musicgen.auth.dto.RegisterRequest;
import com.balians.musicgen.auth.dto.VerifyEmailChangeRequest;
import com.balians.musicgen.auth.dto.VerifyRegistrationRequest;
import com.balians.musicgen.auth.service.AuthService;
import com.balians.musicgen.common.response.StandardSuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String SESSION_HEADER = "X-Session-Token";

    private final AuthService authService;

    @PostMapping("/register")
    public StandardSuccessResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        return StandardSuccessResponse.ok(authService.register(request));
    }

    @PostMapping("/register/verify")
    public StandardSuccessResponse<AuthSessionResponse> verifyRegistration(
            @Valid @RequestBody VerifyRegistrationRequest request
    ) {
        return StandardSuccessResponse.ok(authService.verifyRegistration(request));
    }

    @PostMapping("/login")
    public StandardSuccessResponse<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request) {
        return StandardSuccessResponse.ok(authService.login(request));
    }

    @PostMapping("/google")
    public StandardSuccessResponse<AuthSessionResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        return StandardSuccessResponse.ok(authService.googleAuth(request));
    }

    @GetMapping("/me")
    public StandardSuccessResponse<AuthUserResponse> me(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken
    ) {
        return StandardSuccessResponse.ok(authService.me(sessionToken));
    }

    @PostMapping("/email/change/request")
    public StandardSuccessResponse<OtpChallengeResponse> requestEmailChange(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @Valid @RequestBody ChangeEmailRequest request
    ) {
        return StandardSuccessResponse.ok(authService.requestEmailChange(sessionToken, request));
    }

    @PostMapping("/email/change/verify")
    public StandardSuccessResponse<AuthUserResponse> verifyEmailChange(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @Valid @RequestBody VerifyEmailChangeRequest request
    ) {
        return StandardSuccessResponse.ok(authService.verifyEmailChange(sessionToken, request));
    }

    @PostMapping("/password/change")
    public StandardSuccessResponse<AuthUserResponse> changePassword(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return StandardSuccessResponse.ok(authService.changePassword(sessionToken, request));
    }

    @PostMapping("/password/forgot/request")
    public StandardSuccessResponse<String> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.requestPasswordReset(request);
        return StandardSuccessResponse.ok("code-sent");
    }

    @PostMapping("/password/forgot/verify")
    public StandardSuccessResponse<String> verifyPasswordResetCode(
            @Valid @RequestBody ForgotPasswordVerifyRequest request
    ) {
        return StandardSuccessResponse.ok(authService.verifyPasswordResetCode(request));
    }

    @PostMapping("/password/forgot/reset")
    public StandardSuccessResponse<String> resetPasswordWithCode(
            @Valid @RequestBody ForgotPasswordResetRequest request
    ) {
        authService.resetPasswordWithCode(request);
        return StandardSuccessResponse.ok("password-reset");
    }

    @PostMapping("/google/link")
    public StandardSuccessResponse<AuthUserResponse> linkGoogle(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @Valid @RequestBody GoogleAuthRequest request
    ) {
        return StandardSuccessResponse.ok(authService.linkGoogle(sessionToken, request));
    }

    @PostMapping("/google/unlink")
    public StandardSuccessResponse<AuthUserResponse> unlinkGoogle(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken
    ) {
        return StandardSuccessResponse.ok(authService.unlinkGoogle(sessionToken));
    }

    @PostMapping("/logout")
    public StandardSuccessResponse<String> logout(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken
    ) {
        authService.logout(sessionToken);
        return StandardSuccessResponse.ok("logged-out");
    }
}
