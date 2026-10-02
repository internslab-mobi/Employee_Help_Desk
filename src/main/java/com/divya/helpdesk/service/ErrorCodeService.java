package com.divya.helpdesk.service;

import java.util.Map;

public interface ErrorCodeService {

    Map<String, String> loadAllErrorCodes();
    String getErrorCode(String exceptionType);
    String getErrorMessage(String errorCode);
}

