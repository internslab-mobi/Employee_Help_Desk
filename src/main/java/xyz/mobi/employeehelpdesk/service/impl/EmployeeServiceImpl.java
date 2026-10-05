package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeCreateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.DepartmentManager;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.DuplicateResourceException;
import xyz.mobi.employeehelpdesk.exception.InvalidStateException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.DepartmentAgentRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentManagerRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.service.EmployeeService;
import xyz.mobi.employeehelpdesk.service.helperservice.EmailService;

import java.security.SecureRandom;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final DepartmentManagerRepository departmentManagerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Override
    @Transactional
    public EmployeeCreateResponseDTO createEmployee(CreateEmployeeRequestDTO request) {
        Long currentEmployeeId = authService.getCurrentEmployeeId();
        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + currentEmployeeId
                        )
                );

        if (currentEmployee.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can create employees");
        }

        if (request.email() == null || request.email().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String email = request.email().trim().toLowerCase();
        if (Boolean.TRUE.equals(employeeRepository.existsByEmail(email))) {
            throw new DuplicateResourceException("Employee with email '" + email + "' already exists");
        }

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));
        }

        UserRole role = request.role() != null ? request.role() : UserRole.EMPLOYEE;

        if (role == UserRole.AGENT || role == UserRole.EMPLOYEE) {
            if (request.departmentId() == null) {
                log.warn("Employee creation rejected: departmentId is required for role={}", role);
                throw new BadRequestException("Department is required for " + role + " role");
            }
            if (request.managerId() == null) {
                log.warn("Employee creation rejected: managerId is required for role={}", role);
                throw new BadRequestException("Manager is required for " + role + " role");
            }
        }

        DepartmentManager manager = null;
        if (request.managerId() != null) {
            manager = departmentManagerRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department manager not found with id: " + request.managerId()));

            Long managerDepartmentId = (manager.getEmployee() != null && manager.getEmployee().getDepartment() != null)
                    ? manager.getEmployee().getDepartment().getId()
                    : null;

            if (request.departmentId() == null || managerDepartmentId == null || !request.departmentId().equals(managerDepartmentId)) {
                log.warn("Employee creation rejected: managerId={} does not belong to departmentId={}", request.managerId(), request.departmentId());
                throw new BadRequestException("Selected manager does not belong to the employee's department");
            }

            if (manager.getEmployee() != null && manager.getEmployee().getEmail() != null && manager.getEmployee().getEmail().equalsIgnoreCase(email)) {
                log.warn("Employee creation rejected: manager cannot be assigned to themselves for email={}", email);
                throw new BadRequestException("Manager cannot be assigned to themselves as their own manager");
            }
        }

        String rawPassword = generateSecurePassword();
        String passwordHash = passwordEncoder.encode(rawPassword);

        Employee employee = new Employee();
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName() != null ? request.lastName().trim() : null);
        employee.setEmail(email);
        employee.setPhone(request.phone() != null ? request.phone().trim() : null);
        employee.setDesignation(request.designation() != null ? request.designation().trim() : null);
        employee.setDepartment(department);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setRole(role);
        employee.setPasswordHash(passwordHash);
        employee.setDateOfJoining(request.dateOfJoining() != null ? request.dateOfJoining() : LocalDate.now());
        employee.setTimezone(request.timezone() != null && !request.timezone().isBlank() ? request.timezone().trim() : "UTC");
        employee.setManager(manager);

        // 1. Save Employee first so MySQL/JPA generates the primary-key ID
        Employee savedEmployee = employeeRepository.save(employee);

        // 2. Generate employeeCode using generated Employee ID
        savedEmployee.setEmployeeCode(generateEmployeeCode(savedEmployee.getId()));

        log.info("Employee created successfully: employeeId={}, employeeCode={}, role={}, departmentId={}, managerId={}",
                savedEmployee.getId(),
                savedEmployee.getEmployeeCode(),
                savedEmployee.getRole(),
                department != null ? department.getId() : null,
                manager != null ? manager.getId() : null);

        String subject = "Welcome to Employee Helpdesk - Account Created";
        String message = String.format(
                """
                        Hello %s,
                        
                        Your employee account has been successfully created.
                        
                        Here are your login credentials:
                        Email: %s
                        Initial Password: %s
                        
                        Please use your email and initial password to log in. For security reasons, please change your password after logging in.
                        
                        Best regards,
                        Employee Helpdesk Team""",
                savedEmployee.getFirstName(),
                savedEmployee.getEmail(),
                rawPassword
        );

        emailService.sendNotificationEmail(savedEmployee.getEmail(), subject, message);

        return toCreateResponse(savedEmployee);
    }

    private String generateEmployeeCode(Long employeeId) {
        return String.format("EMP%03d", employeeId);
    }

    private String generateSecurePassword() {
        StringBuilder sb = new StringBuilder(12);
        sb.append(UPPERCASE.charAt(SECURE_RANDOM.nextInt(UPPERCASE.length())));
        sb.append(LOWERCASE.charAt(SECURE_RANDOM.nextInt(LOWERCASE.length())));
        sb.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(SECURE_RANDOM.nextInt(SPECIAL.length())));

        for (int i = 4; i < 12; i++) {
            sb.append(ALL_CHARS.charAt(SECURE_RANDOM.nextInt(ALL_CHARS.length())));
        }

        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> getAllEmployees(Pageable pageable) {

        Long currentEmployeeId = authService.getCurrentEmployeeId();

        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + currentEmployeeId
                        )
                );

        Long departmentId = null;

        if (currentEmployee.getRole() == UserRole.MANAGER) {

            if (currentEmployee.getDepartment() == null) {
                throw new BadRequestException(
                        "Manager is not assigned to a department"
                );
            }

            departmentId = currentEmployee.getDepartment().getId();
        }

        Page<Employee> page = employeeRepository.findAllEmployees(
                departmentId,
                pageable
        );

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> searchEmployees(
            String search,
            Long departmentId,
            EmploymentStatus status,
            UserRole role,
            Pageable pageable
    ) {

        Long currentEmployeeId = authService.getCurrentEmployeeId();

        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + currentEmployeeId
                        )
                );

        if (currentEmployee.getRole() == UserRole.MANAGER) {

            if (currentEmployee.getDepartment() == null) {
                throw new BadRequestException(
                        "Manager is not assigned to a department"
                );
            }

            Long managerDepartmentId =
                    currentEmployee.getDepartment().getId();

            if (departmentId != null
                    && !departmentId.equals(managerDepartmentId)) {

                throw new AccessDeniedException(
                        "You are not authorized to search employees from this department"
                );
            }

            departmentId = managerDepartmentId;
        }

        Page<Employee> page = employeeRepository.searchEmployees(
                departmentId,
                status,
                role,
                search,
                pageable
        );

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(Long id) {

        Long currentEmployeeId = authService.getCurrentEmployeeId();

        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + currentEmployeeId
                        )
                );

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + id
                        )
                );

        if (id.equals(currentEmployeeId)) {
            return toResponse(employee);
        }

        if (currentEmployee.getRole() == UserRole.ADMIN) {
            return toResponse(employee);
        }

        if (currentEmployee.getRole() == UserRole.MANAGER) {

            if (currentEmployee.getDepartment() == null
                    || employee.getDepartment() == null
                    || !currentEmployee.getDepartment().getId()
                    .equals(employee.getDepartment().getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view this employee"
                );
            }

            return toResponse(employee);
        }

        throw new AccessDeniedException(
                "You are not authorized to view this employee"
        );
    }

    @Override
    @Transactional
    public void deactivateEmployee(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + id
                        )
                );

        if (employee.getRole() == UserRole.AGENT) {

            DepartmentAgent agent =
                    departmentAgentRepository
                            .findByEmployeeId(id)
                            .orElse(null);

            if (agent != null && hasActiveTickets(agent)) {
                log.warn("Employee deactivation rejected: agent employeeId={} has active tickets", id);
                throw new InvalidStateException(
                        "Cannot deactivate agent because the agent has active tickets."
                );
            }
        }

        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        log.info("Employee deactivated successfully: employeeId={}", id);
    }

    private EmployeeResponseDTO toResponse(Employee employee) {

        return EmployeeResponseDTO.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .designation(employee.getDesignation())
                .departmentId(
                        employee.getDepartment() != null
                                ? employee.getDepartment().getId()
                                : null
                )
                .departmentName(
                        employee.getDepartment() != null
                                ? employee.getDepartment().getName()
                                : null
                )
                .employmentStatus(employee.getEmploymentStatus())
                .role(employee.getRole())
                .dateOfJoining(employee.getDateOfJoining())
                .dateOfExit(employee.getDateOfExit())
                .timezone(employee.getTimezone())
                .build();
    }

    private EmployeeCreateResponseDTO toCreateResponse(Employee employee) {

        return EmployeeCreateResponseDTO.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .designation(employee.getDesignation())
                .departmentId(
                        employee.getDepartment() != null
                                ? employee.getDepartment().getId()
                                : null
                )
                .departmentName(
                        employee.getDepartment() != null
                                ? employee.getDepartment().getName()
                                : null
                )
                .employmentStatus(employee.getEmploymentStatus())
                .role(employee.getRole())
                .dateOfJoining(employee.getDateOfJoining())
                .dateOfExit(employee.getDateOfExit())
                .timezone(employee.getTimezone())
                .createdAt(employee.getCreatedAt())
                .build();
    }

    private boolean hasActiveTickets(DepartmentAgent agent) {

        String countsJson = agent.getTicketStatusCounts();

        if (countsJson == null || countsJson.isBlank()) {
            return false;
        }

        try {
            JsonNode counts = objectMapper.readTree(countsJson);

            return counts.path("OPEN").asInt() > 0
                    || counts.path("IN_PROGRESS").asInt() > 0
                    || counts.path("ON_HOLD").asInt() > 0
                    || counts.path("REOPENED").asInt() > 0;

        } catch (Exception e) {
            throw new InvalidStateException(
                    "Unable to verify agent ticket status counts."
            );
        }
    }
}