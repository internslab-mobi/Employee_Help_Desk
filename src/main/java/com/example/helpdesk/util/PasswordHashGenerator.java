package com.example.helpdesk.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("Employee@123: " + encoder.encode("Employee@123"));
        System.out.println("Agent@123: " + encoder.encode("Agent@123"));
        System.out.println("Manager@123: " + encoder.encode("Manager@123"));
        System.out.println("Admin@123: " + encoder.encode("Admin@123"));
    }
}
