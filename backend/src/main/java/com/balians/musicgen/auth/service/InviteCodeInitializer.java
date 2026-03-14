package com.balians.musicgen.auth.service;

import com.balians.musicgen.auth.model.InviteCode;
import com.balians.musicgen.auth.repository.InviteCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InviteCodeInitializer {

    private final InviteCodeRepository inviteCodeRepository;
    private final AuthProperties authProperties;

    @PostConstruct
    public void initializeDefaultInviteCode() {
        String defaultInviteCode = authProperties.getDefaultInviteCode();
        if (defaultInviteCode == null || defaultInviteCode.isBlank()) {
            return;
        }

        inviteCodeRepository.findByCode(defaultInviteCode.trim())
                .orElseGet(() -> {
                    log.info("Creating default invite code for local/dev flows");
                    return inviteCodeRepository.save(InviteCode.builder()
                            .code(defaultInviteCode.trim())
                            .active(true)
                            .build());
                });
    }
}
