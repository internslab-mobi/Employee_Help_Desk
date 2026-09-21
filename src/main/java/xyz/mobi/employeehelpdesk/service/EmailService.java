package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.entity.Employee;

public interface EmailService {

    void sendNotificationEmail(Employee recipient, String subject, String message);
}
