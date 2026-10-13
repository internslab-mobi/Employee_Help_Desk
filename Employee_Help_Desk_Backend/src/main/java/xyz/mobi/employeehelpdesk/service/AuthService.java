package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

public interface AuthService {

    Long getCurrentEmployeeId();

    UserRole getCurrentUserRole();

    String getCurrentUserEmail();

    String getCurrentUserTimezone();

    java.time.ZoneId getCurrentUserZoneId();
}
