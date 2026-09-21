package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

public interface CurrentUserService {

    Long getCurrentEmployeeId();

    UserRole getCurrentUserRole();

    String getCurrentUserEmail();
}
