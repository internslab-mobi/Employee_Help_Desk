package com.divya.helpdesk.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AccountActivationResponse {

    private String message;
    private String activationToken;
}
