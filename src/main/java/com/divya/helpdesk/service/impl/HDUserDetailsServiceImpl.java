package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.service.HDUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HDUserDetailsServiceImpl implements HDUserDetailsService {

    private final HDEmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        HDEmployeeEntity employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found with email: " + email));

        return User.builder()
                .username(employee.getEmail())
                .password(employee.getPassword() != null ? employee.getPassword() : "")
                .disabled(!Boolean.TRUE.equals(employee.getEnabled()))
                .authorities("ROLE_" + employee.getRole().name())
                .build();
    }
}
