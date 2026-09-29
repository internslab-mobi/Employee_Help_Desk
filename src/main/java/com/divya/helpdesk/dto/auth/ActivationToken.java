package com.divya.helpdesk.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ActivationToken {

    private final String token;
    private final Instant expiresAt;
}
