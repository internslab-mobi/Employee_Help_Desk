package com.example.helpdesk.config;

import com.example.helpdesk.enums.Role;
import com.example.helpdesk.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeTestUsers();
    }

    private void initializeTestUsers() {
        // Initialize all employees with default credentials based on employee code
        var allEmployees = employeeRepository.findAll();
        log.info("Found {} employees in database", allEmployees.size());
        
        allEmployees.forEach(employee -> {
            String password;
            Role role;
            String employeeCode = employee.getEmployeeCode();

            // Assign role and password based on employee code
            switch (employeeCode) {
                case "EMP001":
                    role = Role.EMPLOYEE;
                    password = "Employee@123";
                    break;
                case "EMP002":
                    role = Role.EMPLOYEE;
                    password = "Employee@123";
                    break;
                case "EMP003":
                    role = Role.AGENT;
                    password = "Agent@123";
                    break;
                case "EMP004":
                    role = Role.MANAGER;
                    password = "Manager@123";
                    break;
                case "EMP005":
                    role = Role.ADMIN;
                    password = "Admin@123";
                    break;
                default:
                    // For any other employees, assign based on designation
                    if (employee.getDesignation() != null && employee.getDesignation().toLowerCase().contains("manager")) {
                        role = Role.MANAGER;
                        password = "Manager@123";
                    } else if (employee.getDesignation() != null && employee.getDesignation().toLowerCase().contains("admin")) {
                        role = Role.ADMIN;
                        password = "Admin@123";
                    } else if (employee.getDesignation() != null && employee.getDesignation().toLowerCase().contains("agent")) {
                        role = Role.AGENT;
                        password = "Agent@123";
                    } else {
                        role = Role.EMPLOYEE;
                        password = "Employee@123";
                    }
            }

            // Always update password and role to ensure consistency
            employee.setPasswordHash(passwordEncoder.encode(password));
            employee.setRole(role);
            employeeRepository.save(employee);
            log.info("Initialized employee {} ({}) with {} role and password {}", 
                employee.getEmployeeCode(), employee.getEmail(), role, password);
        });

        log.info("Test user initialization completed");
    }
}
