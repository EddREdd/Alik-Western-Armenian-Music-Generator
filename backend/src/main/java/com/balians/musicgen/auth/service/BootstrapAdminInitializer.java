package com.balians.musicgen.auth.service;

import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.auth.repository.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer {

    private final AuthProperties authProperties;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initializeBootstrapAdmin() {
        if (!authProperties.isBootstrapAdminEnabled()) {
            return;
        }

        String email = normalizeEmail(authProperties.getBootstrapAdminEmail());
        String password = authProperties.getBootstrapAdminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Bootstrap admin is enabled but email/password is missing");
            return;
        }

        if (userAccountRepository.findByEmail(email).isPresent()) {
            log.info("Bootstrap admin account already exists for {}", email);
            return;
        }

        UserAccount account = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .emailVerified(true)
                .admin(true)
                .unlimitedCredits(true)
                .creditsRemaining(null)
                .creditsUsed(0)
                .songsGenerated(0)
                .frozen(false)
                .freezeReason(null)
                .frozenAt(null)
                .build();

        userAccountRepository.save(java.util.Objects.requireNonNull(account));
        log.info("Bootstrap admin account was created for {}", email);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
