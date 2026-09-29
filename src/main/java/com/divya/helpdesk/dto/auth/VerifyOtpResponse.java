package com.divya.helpdesk.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpResponse {

    private String email;
    private String resetToken;
    private String message;
}