package com.balians.musicgen.email.template;

public enum EmailTemplateType {
    WELCOME("Welcome to Alik"),
    PASSWORD_RESET("Your Alik password reset code"),
    INVITE_CODE("You're invited to Alik");

    private final String defaultSubject;

    EmailTemplateType(String defaultSubject) {
        this.defaultSubject = defaultSubject;
    }

    public String defaultSubject() {
        return defaultSubject;
    }
}
