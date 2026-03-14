package com.balians.musicgen.auth.service;

import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.auth.repository.UserAccountRepository;
import com.balians.musicgen.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountCreditsService {

    private final UserAccountRepository userAccountRepository;

    public void consumeGenerationCredit(UserAccount user) {
        if (Boolean.TRUE.equals(user.getUnlimitedCredits())) {
            return;
        }

        int remaining = user.getCreditsRemaining() == null ? 0 : user.getCreditsRemaining();
        if (remaining <= 0) {
            throw new ConflictException("No credits remaining for this account");
        }

        user.setCreditsRemaining(remaining - 1);
        user.setCreditsUsed((user.getCreditsUsed() == null ? 0 : user.getCreditsUsed()) + 1);
        user.setSongsGenerated((user.getSongsGenerated() == null ? 0 : user.getSongsGenerated()) + 2);
        userAccountRepository.save(user);
    }
}
