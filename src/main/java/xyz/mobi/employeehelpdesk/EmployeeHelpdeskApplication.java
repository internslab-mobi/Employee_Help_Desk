package xyz.mobi.employeehelpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EmployeeHelpdeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeHelpdeskApplication.class, args);
    }

}
