package com.divya.helpdesk.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDTO {

    private String firstName;

    private String lastName;

    private String phone;

    private byte[] profileImage;

    private String timezone;
}