package com.balians.musicgen.auth.service;

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
import com.balians.musicgen.auth.model.InviteCode;
import com.balians.musicgen.auth.model.OtpCode;
import com.balians.musicgen.auth.model.OtpPurpose;
import com.balians.musicgen.auth.model.PasswordResetToken;
import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.auth.model.UserSession;
import com.balians.musicgen.auth.repository.InviteCodeRepository;
import com.balians.musicgen.auth.repository.OtpCodeRepository;
import com.balians.musicgen.auth.repository.PasswordResetTokenRepository;
import com.balians.musicgen.auth.repository.UserAccountRepository;
import com.balians.musicgen.auth.repository.UserSessionRepository;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.ConflictException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.email.SendGridEmailService;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.mapper.GenerationJobMapper;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.GenerationTrack;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final GoogleIdentityService googleIdentityService;
    private final SecurityLogService securityLogService;
    private final SendGridEmailService sendGridEmailService;
    private final GenerationJobRepository generationJobRepository;
    private final GenerationTrackRepository generationTrackRepository;
    private final GenerationJobMapper generationJobMapper;

    public String register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        enforceUserCap();
        ensureEmailAvailable(email);

        boolean isAdmin = isAdminEmail(email);

        InviteCode inviteCode = null;
        if (!isAdmin) {
            inviteCode = inviteCodeRepository.findByCode(request.inviteCode().trim())
                    .orElseThrow(() -> new BadRequestException("Invalid invite code"));
            if (!Boolean.TRUE.equals(inviteCode.getActive())) {
                throw new BadRequestException("Invite code is inactive");
            }
            validateInviteEmailMatch(inviteCode, email);
        }

        UserAccount user = userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(true)
                .admin(isAdmin)
                .unlimitedCredits(isAdmin)
                .creditsRemaining(isAdmin ? null : authProperties.getStandardCredits())
                .creditsUsed(0)
                .songsGenerated(0)
                .frozen(false)
                .build());

        if (inviteCode != null) {
            inviteCode.setActive(false);
            inviteCode.setUsedByUserId(user.getId());
            inviteCode.setUsedAt(Instant.now());
            inviteCodeRepository.save(inviteCode);
        }

        sendWelcomeEmail(user.getEmail());
        securityLogService.log(user.getId(), user.getEmail(), "REGISTERED", "User registered and welcome email sent");
        log.info("Registered user id={} email={}", user.getId(), user.getEmail());
        return "registration-complete";
    }

    public AuthSessionResponse verifyRegistration(VerifyRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found for email: " + email));

        OtpCode otpCode = validateOtp(email, OtpPurpose.REGISTRATION_VERIFICATION, request.otpCode());
        otpCode.setConsumedAt(Instant.now());
        otpCodeRepository.save(otpCode);

        user.setEmailVerified(true);
        userAccountRepository.save(user);
        securityLogService.log(user.getId(), user.getEmail(), "EMAIL_VERIFIED", "Registration email verified");
        sendWelcomeEmail(user.getEmail());
        return createSession(user);
    }

    public AuthSessionResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found for email: " + email));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ConflictException("Password login is not available for this account");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user = userAccountRepository.save(user);
            securityLogService.log(user.getId(), user.getEmail(), "EMAIL_VERIFIED", "Email auto-verified on successful login");
        }
        ensureNotFrozen(user);

        securityLogService.log(user.getId(), user.getEmail(), "LOGIN", "Password login successful");
        return createSession(user);
    }

    public AuthUserResponse me(String sessionToken) {
        return toUserResponse(getAuthenticatedUser(sessionToken));
    }

    public UserAccount requireAuthenticatedUser(String sessionToken) {
        return getAuthenticatedUser(sessionToken);
    }

    public UserAccount requireAdminUser(String sessionToken) {
        UserAccount user = getAuthenticatedUser(sessionToken);
        if (!Boolean.TRUE.equals(user.getAdmin())) {
            throw new ConflictException("Admin access is required");
        }
        return user;
    }

    public OtpChallengeResponse requestEmailChange(String sessionToken, ChangeEmailRequest request) {
        UserAccount user = getAuthenticatedUser(sessionToken);
        String newEmail = normalizeEmail(request.newEmail());
        if (newEmail.equals(user.getEmail())) {
            throw new BadRequestException("New email must be different from the current email");
        }
        ensureEmailAvailable(newEmail);

        user.setPendingEmail(newEmail);
        userAccountRepository.save(user);

        OtpCode otpCode = issueOtp(user.getId(), newEmail, OtpPurpose.EMAIL_CHANGE_VERIFICATION);
        securityLogService.log(user.getId(), user.getEmail(), "EMAIL_CHANGE_REQUESTED", "Email change OTP issued for " + newEmail);
        return toOtpResponse(otpCode);
    }

    public AuthUserResponse verifyEmailChange(String sessionToken, VerifyEmailChangeRequest request) {
        UserAccount user = getAuthenticatedUser(sessionToken);
        if (user.getPendingEmail() == null || user.getPendingEmail().isBlank()) {
            throw new BadRequestException("No pending email change found");
        }

        OtpCode otpCode = validateOtp(user.getPendingEmail(), OtpPurpose.EMAIL_CHANGE_VERIFICATION, request.otpCode());
        otpCode.setConsumedAt(Instant.now());
        otpCodeRepository.save(otpCode);

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setEmailVerified(true);
        UserAccount saved = userAccountRepository.save(user);
        securityLogService.log(saved.getId(), saved.getEmail(), "EMAIL_CHANGED", "Email address updated");
        return toUserResponse(saved);
    }

    public AuthUserResponse changePassword(String sessionToken, ChangePasswordRequest request) {
        UserAccount user = getAuthenticatedUser(sessionToken);
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ConflictException("Password change is not available for this account");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        UserAccount saved = userAccountRepository.save(user);
        securityLogService.log(saved.getId(), saved.getEmail(), "PASSWORD_CHANGED", "Password updated");
        return toUserResponse(saved);
    }

    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No such user, please signup"));

        OtpCode otpCode = issueOtp(user.getId(), email, OtpPurpose.PASSWORD_RESET_VERIFICATION, 5);
        securityLogService.log(user.getId(), user.getEmail(), "PASSWORD_RESET_REQUESTED", "Password reset code issued");
        sendPasswordResetEmail(user.getEmail(), otpCode.getCode());
    }

    public String verifyPasswordResetCode(ForgotPasswordVerifyRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No such user, please signup"));

        OtpCode otpCode = validateOtp(email, OtpPurpose.PASSWORD_RESET_VERIFICATION, request.otpCode());
        if (!user.getId().equals(otpCode.getUserId())) {
            throw new BadRequestException("Wrong code, please click resend for a new one");
        }

        otpCode.setConsumedAt(Instant.now());
        otpCodeRepository.save(otpCode);
        // Remove all password-reset OTP codes after successful verification.
        otpCodeRepository.deleteByEmailAndPurpose(email, OtpPurpose.PASSWORD_RESET_VERIFICATION);
        return issuePasswordResetToken(user);
    }

    public void resetPasswordWithCode(ForgotPasswordResetRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No such user, please signup"));

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByEmailAndToken(email, request.resetToken().trim())
                .orElseThrow(() -> new BadRequestException("Reset link/code is invalid, please resend"));
        if (resetToken.getConsumedAt() != null || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset link/code expired, please resend");
        }
        if (!user.getId().equals(resetToken.getUserId())) {
            throw new BadRequestException("Reset link/code is invalid, please resend");
        }

        resetToken.setConsumedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.deleteByEmail(email);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setEmailVerified(true);
        userAccountRepository.save(user);
        userSessionRepository.deleteByUserId(user.getId());
        otpCodeRepository.deleteByEmailAndPurpose(email, OtpPurpose.PASSWORD_RESET_VERIFICATION);
        securityLogService.log(user.getId(), user.getEmail(), "PASSWORD_RESET_COMPLETED", "Password reset via email code");
    }

    public AuthSessionResponse googleAuth(GoogleAuthRequest request) {
        ensureGoogleEnabled();
        GoogleIdentityService.GoogleIdentity googleIdentity = googleIdentityService.verifyIdToken(request.idToken());
        if (googleIdentity.email() == null || googleIdentity.email().isBlank()) {
            throw new BadRequestException("Google account email is unavailable");
        }

        UserAccount user = userAccountRepository.findByGoogleSubject(googleIdentity.subject())
                .orElseGet(() -> resolveGoogleUser(googleIdentity, request.inviteCode()));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user = userAccountRepository.save(user);
        }
        ensureNotFrozen(user);

        securityLogService.log(user.getId(), user.getEmail(), "GOOGLE_LOGIN", "Google sign in successful");
        return createSession(user);
    }

    public AuthUserResponse linkGoogle(String sessionToken, GoogleAuthRequest request) {
        ensureGoogleEnabled();
        UserAccount user = getAuthenticatedUser(sessionToken);
        GoogleIdentityService.GoogleIdentity googleIdentity = googleIdentityService.verifyIdToken(request.idToken());

        userAccountRepository.findByGoogleSubject(googleIdentity.subject())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("Google account is already linked to another user");
                });

        user.setGoogleSubject(googleIdentity.subject());
        user.setGoogleEmail(googleIdentity.email());
        UserAccount saved = userAccountRepository.save(user);
        securityLogService.log(saved.getId(), saved.getEmail(), "GOOGLE_LINKED", "Google account linked");
        return toUserResponse(saved);
    }

    public AuthUserResponse unlinkGoogle(String sessionToken) {
        UserAccount user = getAuthenticatedUser(sessionToken);
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
        if (!hasPassword || !hasEmail) {
            throw new ConflictException("Cannot unlink Google when password or email login is not available");
        }
        if (user.getGoogleSubject() == null || user.getGoogleSubject().isBlank()) {
            throw new BadRequestException("Google account is not linked");
        }

        user.setGoogleSubject(null);
        user.setGoogleEmail(null);
        UserAccount saved = userAccountRepository.save(user);
        securityLogService.log(saved.getId(), saved.getEmail(), "GOOGLE_UNLINKED", "Google account unlinked");
        return toUserResponse(saved);
    }

    public void logout(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }

        userSessionRepository.findByToken(sessionToken).ifPresent(session -> {
            UserAccount user = userAccountRepository.findById(session.getUserId()).orElse(null);
            userSessionRepository.delete(session);
            if (user != null) {
                securityLogService.log(user.getId(), user.getEmail(), "LOGOUT", "Session terminated");
            }
        });
    }

    private UserAccount getAuthenticatedUser(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new BadRequestException("X-Session-Token header is required");
        }

        UserSession session = userSessionRepository.findByToken(sessionToken)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (session.getExpiresAt().isBefore(Instant.now())) {
            userSessionRepository.delete(session);
            throw new ConflictException("Session has expired");
        }

        session.setLastUsedAt(Instant.now());
        userSessionRepository.save(session);

        UserAccount user = userAccountRepository.findById(session.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found for session"));
        ensureNotFrozen(user);
        return user;
    }

    private AuthSessionResponse createSession(UserAccount user) {
        Instant now = Instant.now();
        UserSession session = userSessionRepository.save(UserSession.builder()
                .userId(user.getId())
                .token(generateSessionToken())
                .createdAt(now)
                .lastUsedAt(now)
                .expiresAt(now.plusSeconds(authProperties.getSessionTtlHours() * 3600))
                .build());
        List<GenerationJobResponse> songs = getSongsForUser(user.getId());

        return new AuthSessionResponse(
                toUserResponse(user),
                session.getToken(),
                session.getExpiresAt(),
                songs
        );
    }

    private List<GenerationJobResponse> getSongsForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }

        List<GenerationJob> jobs = generationJobRepository
                .findByOwnerUserIdAndHiddenFromLibraryFalseOrderByCreatedAtDesc(userId);

        return jobs.stream()
                .map(job -> {
                    List<GenerationTrack> tracks = generationTrackRepository
                            .findByGenerationJobIdOrderByTrackIndexAsc(job.getId());
                    return generationJobMapper.toResponse(job, tracks);
                })
                .toList();
    }

    private OtpCode issueOtp(String userId, String email, OtpPurpose purpose) {
        return issueOtp(userId, email, purpose, 6);
    }

    private OtpCode issueOtp(String userId, String email, OtpPurpose purpose, int digits) {
        otpCodeRepository.findByEmailAndPurposeAndConsumedAtIsNull(email, purpose)
                .forEach(code -> {
                    code.setConsumedAt(Instant.now());
                    otpCodeRepository.save(code);
                });

        Instant expiresAt = Instant.now().plusSeconds(authProperties.getOtpTtlMinutes() * 60);
        int maxValue = (int) Math.pow(10, Math.max(1, digits));
        String pattern = "%0" + Math.max(1, digits) + "d";
        String otpValue = String.format(pattern, new SecureRandom().nextInt(maxValue));

        OtpCode otpCode = otpCodeRepository.save(OtpCode.builder()
                .userId(userId)
                .email(email)
                .purpose(purpose)
                .code(otpValue)
                .expiresAt(expiresAt)
                .build());

        log.info("Issued OTP purpose={} email={} code={}", purpose, email, otpValue);
        return otpCode;
    }

    private void sendPasswordResetEmail(String email, String otpCode) {
        String subject = "Alik password reset code";
        String body = "Use this 5-digit code to reset your password: "
                + otpCode
                + "\n\nIf you did not request this, you can ignore this email.";
        boolean sent = sendGridEmailService.sendTextEmail(email, subject, body);
        if (!sent) {
            log.warn("Password reset email failed for {}", email);
        }
    }

    private void sendWelcomeEmail(String email) {
        String subject = "welcome form Alik";
        String body = "you become Alik pilot users thank you.";
        boolean sent = sendGridEmailService.sendTextEmail(email, subject, body);
        if (!sent) {
            log.warn("Welcome email failed for {}", email);
        }
    }

    private String issuePasswordResetToken(UserAccount user) {
        passwordResetTokenRepository.deleteByEmail(user.getEmail());
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .token(token)
                .expiresAt(Instant.now().plusSeconds(15 * 60))
                .build());
        return token;
    }

    private OtpCode validateOtp(String email, OtpPurpose purpose, String otpCode) {
        OtpCode storedOtp = otpCodeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new BadRequestException("OTP code was not found"));

        if (storedOtp.getConsumedAt() != null) {
            throw new ConflictException("OTP code has already been used");
        }
        if (storedOtp.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("OTP code has expired");
        }
        if (!storedOtp.getCode().equals(otpCode.trim())) {
            throw new BadRequestException("OTP code is invalid");
        }

        return storedOtp;
    }

    private void ensureEmailAvailable(String email) {
        if (userAccountRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email is already registered");
        }
    }

    private void validateInviteEmailMatch(InviteCode inviteCode, String email) {
        if (inviteCode == null || email == null || email.isBlank()) {
            return;
        }
        String assignedEmail = inviteCode.getLastSentToEmail();
        if (assignedEmail != null && !assignedEmail.isBlank() && !assignedEmail.equalsIgnoreCase(email)) {
            throw new BadRequestException("Invite code and email do not match");
        }
    }

    private UserAccount resolveGoogleUser(GoogleIdentityService.GoogleIdentity googleIdentity, String inviteCodeValue) {
        return userAccountRepository.findByEmail(googleIdentity.email())
                .map(existingUser -> {
                    existingUser.setGoogleSubject(googleIdentity.subject());
                    existingUser.setGoogleEmail(googleIdentity.email());
                    existingUser.setEmailVerified(true);
                    return userAccountRepository.save(existingUser);
                })
                .orElseGet(() -> createGoogleOnlyUser(googleIdentity, inviteCodeValue));
    }

    private UserAccount createGoogleOnlyUser(
            GoogleIdentityService.GoogleIdentity googleIdentity,
            String inviteCodeValue
    ) {
        enforceUserCap();
        InviteCode inviteCode = consumeInviteCode(inviteCodeValue);
        validateInviteEmailMatch(inviteCode, googleIdentity.email());
        boolean admin = isAdminEmail(googleIdentity.email());

        UserAccount user = userAccountRepository.save(UserAccount.builder()
                .email(googleIdentity.email())
                .passwordHash(null)
                .emailVerified(true)
                .googleSubject(googleIdentity.subject())
                .googleEmail(googleIdentity.email())
                .admin(admin)
                .unlimitedCredits(admin)
                .creditsRemaining(admin ? null : authProperties.getStandardCredits())
                .creditsUsed(0)
                .songsGenerated(0)
                .frozen(false)
                .build());

        inviteCode.setActive(false);
        inviteCode.setUsedByUserId(user.getId());
        inviteCode.setUsedAt(Instant.now());
        inviteCodeRepository.save(inviteCode);

        return user;
    }

    private InviteCode consumeInviteCode(String inviteCodeValue) {
        if (inviteCodeValue == null || inviteCodeValue.isBlank()) {
            throw new BadRequestException("Invite code is required for first-time Google sign in");
        }

        InviteCode inviteCode = inviteCodeRepository.findByCode(inviteCodeValue.trim())
                .orElseThrow(() -> new BadRequestException("Invalid invite code"));
        if (!Boolean.TRUE.equals(inviteCode.getActive())) {
            throw new BadRequestException("Invite code is inactive");
        }
        return inviteCode;
    }

    private OtpChallengeResponse toOtpResponse(OtpCode otpCode) {
        return new OtpChallengeResponse(
                otpCode.getEmail(),
                otpCode.getPurpose().name(),
                otpCode.getExpiresAt(),
                authProperties.isExposeOtpInResponse() ? otpCode.getCode() : null
        );
    }

    private AuthUserResponse toUserResponse(UserAccount user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getPasswordHash() != null && !user.getPasswordHash().isBlank(),
                user.getGoogleSubject() != null && !user.getGoogleSubject().isBlank(),
                Boolean.TRUE.equals(user.getAdmin()),
                Boolean.TRUE.equals(user.getUnlimitedCredits()),
                user.getCreditsRemaining(),
                user.getCreditsUsed(),
                user.getSongsGenerated(),
                Boolean.TRUE.equals(user.getFrozen()),
                user.getFreezeReason()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private void ensureGoogleEnabled() {
        if (!authProperties.isGoogleOauthEnabled()) {
            throw new BadRequestException("Google OAuth is not configured");
        }
        requireText(authProperties.getGoogleClientId(), "Google client ID is not configured");
    }

    private String generateSessionToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private void enforceUserCap() {
        if (userAccountRepository.countByEmailVerifiedTrue() >= authProperties.getMaxUsers()) {
            throw new ConflictException("Maximum user capacity has been reached");
        }
    }

    private boolean isAdminEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Set<String> adminEmails = Arrays.stream(authProperties.getAdminEmails().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return adminEmails.contains(email.trim().toLowerCase());
    }

    private void ensureNotFrozen(UserAccount user) {
        if (Boolean.TRUE.equals(user.getFrozen())) {
            throw new ConflictException("This account is frozen");
        }
    }
}
