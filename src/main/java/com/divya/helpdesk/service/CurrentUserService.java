package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDEmployeeEntity;

public interface CurrentUserService {

    Long getEmployeeId();

    HDEmployeeEntity getCurrentEmployee();
}
