package com.divya.helpdesk.service;

import java.util.Map;

public interface HDErrorCodeService {

    Map<String, String> loadAllErrorCodes();
    String getErrorCode(String exceptionType);
    String getErrorMessage(String errorCode);
}

