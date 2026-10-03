package com.example.helpdesk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerCustomController {

    @GetMapping("/swagger-custom")
    public String customSwagger() {
        return "swagger-custom";
    }
}




