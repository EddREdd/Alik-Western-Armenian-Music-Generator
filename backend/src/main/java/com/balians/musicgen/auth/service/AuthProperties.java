package com.balians.musicgen.auth.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private long sessionTtlHours = 168;
    private long otpTtlMinutes = 10;
    private boolean exposeOtpInResponse = false;
    private String defaultInviteCode = "";
    private boolean googleOauthEnabled = false;
    private String googleClientId = "";
    private String googleTokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo";
    private int maxUsers = 100;
    private int standardCredits = 50;
    private String adminEmails = "";
}
