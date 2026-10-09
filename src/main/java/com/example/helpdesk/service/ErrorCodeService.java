package com.example.helpdesk.service;

import com.example.helpdesk.entity.ErrorCode;

import java.util.Optional;

public interface ErrorCodeService {

    Optional<ErrorCode> findByCode(String code);

    ErrorCode getByCode(String code);
}




