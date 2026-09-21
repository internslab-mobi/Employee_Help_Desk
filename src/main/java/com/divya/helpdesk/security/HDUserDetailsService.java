package com.divya.helpdesk.security;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HDUserDetailsService implements UserDetailsService{
    private final HDEmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        HDEmployee employee = employeeRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return User.builder()
                .username(employee.getEmail())
                .password(employee.getPassword())
                .disabled(!Boolean.TRUE.equals(employee.getEnabled()))
                .authorities("ROLE_"+employee.getRole())
                .build();
    }
}
