package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.user.*;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDDepartment;
import com.divya.helpdesk.exception.DuplicateResourceException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.EmployeeMapper;
import com.divya.helpdesk.repository.HDDepartmentRepository;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.service.EmailService;
import com.divya.helpdesk.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.divya.helpdesk.util.TimezoneUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final HDEmployeeRepository employeeRepository;
    private final HDDepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS;
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        sb.append(UPPER.charAt(secureRandom.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(secureRandom.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));

        for (int i = 3; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(ALL_CHARS.charAt(secureRandom.nextInt(ALL_CHARS.length())));
        }

        char[] array = sb.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }

    // UPDATE EMPLOYEE - ADMIN - PUT
    @Override
    public EmployeeUpdateResponse updateEntireEmployee(Long employeeId, UpdateEmployeeRequest request) {

        HDEmployee employee = getEmployeeEntity(employeeId);

        if (request.getFirstName() != null) {
            employee.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            employee.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getDesignation() != null) {
            employee.setDesignation(request.getDesignation());
        }
        if (request.getDepartmentId() != null) {
            HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            employee.setDepartment(department);
        }
        if (request.getRole() != null) {
            employee.setRole(request.getRole());
        }
        if (request.getEmploymentStatus() != null) {
            employee.setEmploymentStatus(request.getEmploymentStatus());
        }
        if (request.getDateOfJoining() != null) {
            employee.setDateOfJoining(request.getDateOfJoining());
        }
        if (request.getEnabled() != null) {
            employee.setEnabled(request.getEnabled());
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            String tz = request.getTimezone().trim();
            TimezoneUtil.validateAndGetZoneId(tz);
            employee.setTimezone(tz);
        }

        return EmployeeMapper.mapToUpdateResponse(employee);
    }

    // PATCH EMPLOYEE - Single endpoint handling all eligible patchable fields
    @Override
    public EmployeeUpdateResponse patchEmployee(Long employeeId, EmployeePatchRequest request) {

        HDEmployee employee = getEmployeeEntity(employeeId);

        // Protected fields like employeeCode, id, createdAt, updatedAt are NOT modified here
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(employee.getEmail()) && employeeRepository.existsByEmail(newEmail)) {
                throw new DuplicateResourceException("Employee already exists with email: " + newEmail);
            }
            employee.setEmail(newEmail);
        }
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            employee.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            employee.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getDesignation() != null) {
            employee.setDesignation(request.getDesignation());
        }
        if (request.getDepartmentId() != null) {
            HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            employee.setDepartment(department);
        }
        if (request.getRole() != null) {
            employee.setRole(request.getRole());
        }
        if (request.getEmploymentStatus() != null) {
            employee.setEmploymentStatus(request.getEmploymentStatus());
        }
        if (request.getDateOfJoining() != null) {
            employee.setDateOfJoining(request.getDateOfJoining());
        }
        if (request.getDateOfExit() != null) {
            employee.setDateOfExit(request.getDateOfExit());
        }
        if (request.getEnabled() != null) {
            employee.setEnabled(request.getEnabled());
        }
        if (request.getActivated() != null) {
            employee.setActivated(request.getActivated());
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            String tz = request.getTimezone().trim();
            TimezoneUtil.validateAndGetZoneId(tz);
            employee.setTimezone(tz);
        }
        HDEmployee savedEmployee = employeeRepository.save(employee);

        return EmployeeMapper.mapToUpdateResponse(savedEmployee);
    }

    // UPDATE MY PROFILE - EMPLOYEE - PATCH
    @Override
    public EmployeeUpdateResponse updateMyProfile(Long employeeId, UpdateProfileRequest request) {

        HDEmployee employee = getEmployeeEntity(employeeId);

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            employee.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            employee.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getProfileImage() != null) {
            employee.setProfileImage(request.getProfileImage());
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            String tz = request.getTimezone().trim();
            TimezoneUtil.validateAndGetZoneId(tz);
            employee.setTimezone(tz);
        }

        return EmployeeMapper.mapToUpdateResponse(employee);
    }

    // CREATE EMPLOYEE - AUTO GENERATED CONCURRENCY-SAFE EMPLOYEE CODE (EMP_001, EMP_002...)
    @Override
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {

        if (employeeRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("Employee already exists with email: " + request.getEmail());
        }

        HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        String generatedCode = generateNextEmployeeCode();
        String temporaryPassword = generateTemporaryPassword();

        String tz = (request.getTimezone() != null && !request.getTimezone().isBlank())
                ? request.getTimezone().trim()
                : TimezoneUtil.DEFAULT_TIMEZONE;
        TimezoneUtil.validateAndGetZoneId(tz);

        HDEmployee employee = new HDEmployee();
        employee.setEmployeeCode(generatedCode);
        employee.setEmail(request.getEmail().trim().toLowerCase());
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setPhone(request.getPhone());
        employee.setDesignation(request.getDesignation());
        employee.setDepartment(department);
        employee.setRole(request.getRole());
        employee.setEmploymentStatus(request.getEmploymentStatus());
        employee.setDateOfJoining(request.getDateOfJoining());
        employee.setTimezone(tz);

        employee.setPassword(passwordEncoder.encode(temporaryPassword));
        employee.setActivated(false);
        employee.setEnabled(true);

        HDEmployee savedEmployee = employeeRepository.save(employee);
        log.info("Successfully created employee with auto-generated code: {} and email: {}", savedEmployee.getEmployeeCode(), savedEmployee.getEmail());

        // Send temporary-password onboarding email
        String fullName = savedEmployee.getFirstName() + " " + savedEmployee.getLastName();
        emailService.sendTemporaryPasswordEmail(savedEmployee.getEmail(), fullName.trim(), savedEmployee.getEmployeeCode(), temporaryPassword);

        return EmployeeMapper.mapToResponse(savedEmployee);
    }

    // GET EMPLOYEE BY ID
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long employeeId) {
        return EmployeeMapper.mapToResponse(getEmployeeEntity(employeeId));
    }

    // GET MY PROFILE
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getMyProfile(Long employeeId) {
        return EmployeeMapper.mapToResponse(getEmployeeEntity(employeeId));
    }


    // COMMON / HELPER METHODS
    private HDEmployee getEmployeeEntity(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }


    private synchronized String generateNextEmployeeCode() {
        Pageable topOne = PageRequest.of(0, 1);
        List<HDEmployee> lastList = employeeRepository.findLastEmployeeForUpdate(topOne);
        long nextNumber = 1;
        if (!lastList.isEmpty() && lastList.get(0).getEmployeeCode() != null) {
            nextNumber = extractNextNumber(lastList.get(0).getEmployeeCode());
        }

        String code = String.format("EMP_%03d", nextNumber);
        while (employeeRepository.existsByEmployeeCode(code)) {
            nextNumber++;
            code = String.format("EMP_%03d", nextNumber);
        }
        return code;
    }

    private long extractNextNumber(String code) {
        if (code == null || code.isBlank()) {
            return 1;
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(code);
        long max = 0;
        while (matcher.find()) {
            try {
                long val = Long.parseLong(matcher.group(1));
                if (val > max) {
                    max = val;
                }
            } catch (NumberFormatException ignored) {}
        }
        return max > 0 ? max + 1 : 1;
    }
}