package xyz.mobi.notificationservice.service;

import xyz.mobi.notificationservice.dto.EmailRequestDTO;

public interface EmailService {

    void sendEmail(EmailRequestDTO request);

}
