package com.balians.musicgen.polling.model;

public enum PollAttemptOutcome {
    PARTIAL,
    TERMINAL_SUCCESS,
    TERMINAL_FAILURE,
    PROVIDER_ERROR,
    MALFORMED_RESPONSE,
    SKIPPED
}
