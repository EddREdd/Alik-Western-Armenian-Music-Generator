package com.balians.musicgen.auth.service;

import com.balians.musicgen.auth.model.SecurityLog;
import com.balians.musicgen.auth.repository.SecurityLogRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityLogService {

    private final SecurityLogRepository securityLogRepository;

    public void log(String userId, String email, String eventType, String details) {
        securityLogRepository.save(SecurityLog.builder()
                .userId(userId)
                .email(email)
                .eventType(eventType)
                .details(details)
                .occurredAt(Instant.now())
                .build());
    }

    public List<SecurityLog> listForUser(String userId) {
        return securityLogRepository.findByUserIdOrderByOccurredAtDesc(userId);
    }
}
