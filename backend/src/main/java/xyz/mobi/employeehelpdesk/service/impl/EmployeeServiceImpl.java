package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeCreateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.UpdateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.DepartmentManager;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;
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
import xyz.mobi.employeehelpdesk.repository.TicketAttachmentRepository;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.service.EmployeeService;
import xyz.mobi.employeehelpdesk.service.helperservice.EmailService;
import xyz.mobi.employeehelpdesk.validator.TicketAttachmentValidator;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TicketAttachmentValidator ticketAttachmentValidator;

    @Override
    @Transactional
    public EmployeeCreateResponseDTO createEmployee(CreateEmployeeRequestDTO request) {

        // 1. Get currently logged-in employee
        Long currentEmployeeId = authService.getCurrentEmployeeId();

        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + currentEmployeeId
                ));

        // 2. Only ADMIN can create employees
        if (currentEmployee.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can create employees");
        }

        // 3. Validate email
        if (request.email() == null || request.email().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String email = request.email().trim().toLowerCase();

        if (Boolean.TRUE.equals(employeeRepository.existsByEmail(email))) {
            throw new DuplicateResourceException(
                    "Employee with email '" + email + "' already exists"
            );
        }

        // 4. Determine role
        UserRole role = request.role() != null
                ? request.role()
                : UserRole.EMPLOYEE;

        // 5. Validate department
        Department department = null;

        if (request.departmentId() != null) {

            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + request.departmentId()
                    ));
        }

        // 6. Validate department and manager requirements
        //
        // EMPLOYEE and AGENT:
        // departmentId -> required
        // managerId    -> required
        //
        // MANAGER:
        // managerId    -> must NOT be provided
        //
        // ADMIN:
        // managerId    -> must NOT be provided

        if (role == UserRole.AGENT || role == UserRole.EMPLOYEE) {

            if (request.departmentId() == null) {
                log.warn(
                        "Employee creation rejected: departmentId is required for role={}",
                        role
                );

                throw new BadRequestException(
                        "Department is required for " + role + " role"
                );
            }

            if (request.managerId() == null) {
                log.warn(
                        "Employee creation rejected: managerId is required for role={}",
                        role
                );

                throw new BadRequestException(
                        "Manager is required for " + role + " role"
                );
            }
        }

        // Manager cannot have another manager
        if (role == UserRole.MANAGER && request.managerId() != null) {

            log.warn(
                    "Manager creation rejected: managerId must be null for MANAGER role"
            );

            throw new BadRequestException(
                    "Manager cannot be assigned to a MANAGER"
            );
        }

        // Admin cannot have a manager
        if (role == UserRole.ADMIN && request.managerId() != null) {

            log.warn(
                    "Admin creation rejected: managerId must be null for ADMIN role"
            );

            throw new BadRequestException(
                    "Manager cannot be assigned to an ADMIN"
            );
        }

        // 7. Validate manager
        DepartmentManager manager = null;

        if (request.managerId() != null) {

            manager = departmentManagerRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department manager not found with id: "
                                    + request.managerId()
                    ));

            // Get the manager's department
            Long managerDepartmentId =
                    (manager.getEmployee() != null
                            && manager.getEmployee().getDepartment() != null)
                            ? manager.getEmployee().getDepartment().getId()
                            : null;

            // Manager must belong to the same department
            if (request.departmentId() == null
                    || managerDepartmentId == null
                    || !request.departmentId().equals(managerDepartmentId)) {

                log.warn(
                        "Employee creation rejected: managerId={} does not belong to departmentId={}",
                        request.managerId(),
                        request.departmentId()
                );

                throw new BadRequestException(
                        "Selected manager does not belong to the employee's department"
                );
            }

            // Manager cannot be the employee being created
            if (manager.getEmployee() != null
                    && manager.getEmployee().getEmail() != null
                    && manager.getEmployee().getEmail().equalsIgnoreCase(email)) {

                log.warn(
                        "Employee creation rejected: manager cannot be assigned to themselves for email={}",
                        email
                );

                throw new BadRequestException(
                        "Manager cannot be assigned to themselves as their own manager"
                );
            }
        }

        // 8. Generate secure password
        String rawPassword = generateSecurePassword();

        String passwordHash = passwordEncoder.encode(rawPassword);

        // 9. Create Employee entity
        Employee employee = new Employee();

        employee.setFirstName(request.firstName().trim());

        employee.setLastName(
                request.lastName() != null
                        ? request.lastName().trim()
                        : null
        );

        employee.setEmail(email);

        employee.setPhone(
                request.phone() != null
                        ? request.phone().trim()
                        : null
        );

        employee.setDesignation(
                request.designation() != null
                        ? request.designation().trim()
                        : null
        );

        employee.setDepartment(department);

        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);

        employee.setRole(role);

        employee.setPasswordHash(passwordHash);

        employee.setDateOfJoining(
                request.dateOfJoining() != null
                        ? request.dateOfJoining()
                        : LocalDate.now()
        );

        employee.setTimezone(
                request.timezone() != null
                        && !request.timezone().isBlank()
                        ? request.timezone().trim()
                        : "UTC"
        );

        employee.setManager(manager);

        // 10. Save employee first
        // MySQL/JPA generates the employee ID
        Employee savedEmployee = employeeRepository.save(employee);

        // 11. Generate employee code using generated ID
        savedEmployee.setEmployeeCode(
                generateEmployeeCode(savedEmployee.getId())
        );

        // Save employee code
        savedEmployee = employeeRepository.save(savedEmployee);

        log.info(
                "Employee created successfully: employeeId={}, employeeCode={}, role={}, departmentId={}, managerId={}",
                savedEmployee.getId(),
                savedEmployee.getEmployeeCode(),
                savedEmployee.getRole(),
                department != null ? department.getId() : null,
                manager != null ? manager.getId() : null
        );

        // 12. Send welcome email
        String subject =
                "Welcome to Employee Helpdesk - Account Created";

        String message = String.format(
                """
                Hello %s,
    
                Your employee account has been successfully created.
    
                Here are your login credentials:
                Email: %s
                Initial Password: %s
    
                Please use your email and initial password to log in.
                For security reasons, please change your password after logging in.
    
                Best regards,
                Employee Helpdesk Team
                """,
                savedEmployee.getFirstName(),
                savedEmployee.getEmail(),
                rawPassword
        );

        emailService.sendNotificationEmail(
                savedEmployee.getEmail(),
                subject,
                message
        );

        // 13. Return response
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + currentEmployeeId));

        Long departmentId = null;

        if (currentEmployee.getRole() == UserRole.MANAGER) {

            if (currentEmployee.getDepartment() == null) {
                throw new BadRequestException(
                        "Manager is not assigned to a department");
            }

            departmentId = currentEmployee.getDepartment().getId();
        }

        Page<Employee> page = employeeRepository.findAllEmployees(
                departmentId,
                pageable);

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> searchEmployees(
            String search,
            Long departmentId,
            EmploymentStatus status,
            UserRole role,
            Pageable pageable) {

        Long currentEmployeeId = authService.getCurrentEmployeeId();

        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + currentEmployeeId));

        if (currentEmployee.getRole() == UserRole.MANAGER) {

            if (currentEmployee.getDepartment() == null) {
                throw new BadRequestException(
                        "Manager is not assigned to a department");
            }

            Long managerDepartmentId = currentEmployee.getDepartment().getId();

            if (departmentId != null
                    && !departmentId.equals(managerDepartmentId)) {

                throw new AccessDeniedException(
                        "You are not authorized to search employees from this department");
            }

            departmentId = managerDepartmentId;
        }

        Page<Employee> page = employeeRepository.searchEmployees(
                departmentId,
                status,
                role,
                search,
                pageable);

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(Long id) {

        Long currentEmployeeId = authService.getCurrentEmployeeId();

        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + currentEmployeeId));

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + id));

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
                        "You are not authorized to view this employee");
            }

            return toResponse(employee);
        }

        throw new AccessDeniedException(
                "You are not authorized to view this employee");
    }

    @Override
    @Transactional
    public void deactivateEmployee(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + id));

        if (employee.getRole() == UserRole.AGENT) {

            DepartmentAgent agent = departmentAgentRepository
                    .findByEmployeeId(id)
                    .orElse(null);

            if (agent != null && hasActiveTickets(agent)) {
                log.warn("Employee deactivation rejected: agent employeeId={} has active tickets", id);
                throw new InvalidStateException(
                        "Cannot deactivate agent because the agent has active tickets.");
            }
        }

        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        employee.setDateOfExit(LocalDate.now());
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
                                : null)
                .departmentName(
                        employee.getDepartment() != null
                                ? employee.getDepartment().getName()
                                : null)
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
                                : null)
                .departmentName(
                        employee.getDepartment() != null
                                ? employee.getDepartment().getName()
                                : null)
                .employmentStatus(employee.getEmploymentStatus())
                .role(employee.getRole())
                .dateOfJoining(employee.getDateOfJoining())
                .dateOfExit(employee.getDateOfExit())
                .timezone(employee.getTimezone())
                .createdAt(employee.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public EmployeeResponseDTO updateEmployee(
            Long employeeId,
            UpdateEmployeeRequestDTO request,
            MultipartFile profileImage
    ) throws IOException {
        if (employeeId == null) {
            throw new BadRequestException("Employee ID is required");
        }

        final UpdateEmployeeRequestDTO effectiveRequest = request != null
                ? request
                : UpdateEmployeeRequestDTO.builder().build();

        // 2. Fetch current authenticated employee
        Long currentEmployeeId = authService.getCurrentEmployeeId();
        Employee currentEmployee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + currentEmployeeId));

        // 3. Fetch target employee to update
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        boolean isAdmin = currentEmployee.getRole() == UserRole.ADMIN;

        // 4. Role-based Scope & Field-level Authorization
        if (!isAdmin) {
            // EMPLOYEE, AGENT, MANAGER can only update their own profile
            if (!currentEmployeeId.equals(employeeId)) {
                log.warn("Employee update rejected: user {} is not authorized to update employee {}", currentEmployeeId, employeeId);
                throw new AccessDeniedException("You are not authorized to update another employee's profile");
            }

            // Reject attempts to modify unauthorized/administrative fields
            if (effectiveRequest.email() != null) {
                throw new AccessDeniedException("You are not authorized to update email");
            }
            if (effectiveRequest.departmentId() != null) {
                throw new AccessDeniedException("You are not authorized to update department");
            }
            if (effectiveRequest.managerId() != null) {
                throw new AccessDeniedException("You are not authorized to update manager");
            }
            if (effectiveRequest.role() != null) {
                throw new AccessDeniedException("You are not authorized to update role");
            }
            if (effectiveRequest.timezone() != null) {
                throw new AccessDeniedException("You are not authorized to update timezone");
            }
        }

        // 5. Update common allowed fields (firstName, lastName, phone, designation)
        if (effectiveRequest.firstName() != null) {
            if (effectiveRequest.firstName().trim().isBlank()) {
                throw new BadRequestException("First name cannot be blank");
            }
            employee.setFirstName(effectiveRequest.firstName().trim());
        }

        if (effectiveRequest.lastName() != null) {
            employee.setLastName(effectiveRequest.lastName().trim().isEmpty() ? null : effectiveRequest.lastName().trim());
        }

        if (effectiveRequest.phone() != null) {
            String trimmedPhone = effectiveRequest.phone().trim();
            employee.setPhone(trimmedPhone.isEmpty() ? null : trimmedPhone);
        }

        if (effectiveRequest.designation() != null) {
            String trimmedDesignation = effectiveRequest.designation().trim();
            employee.setDesignation(trimmedDesignation.isEmpty() ? null : trimmedDesignation);
        }

        // 6. Update ADMIN-only fields
        if (isAdmin) {
            if (effectiveRequest.email() != null) {
                if (effectiveRequest.email().trim().isBlank()) {
                    throw new BadRequestException("Email cannot be blank");
                }
                String email = effectiveRequest.email().trim().toLowerCase();
                if (!email.equalsIgnoreCase(employee.getEmail())) {
                    if (Boolean.TRUE.equals(employeeRepository.existsByEmail(email))) {
                        throw new DuplicateResourceException("Employee with email '" + email + "' already exists");
                    }
                    employee.setEmail(email);
                }
            }

            if (effectiveRequest.departmentId() != null) {
                Department department = departmentRepository.findById(effectiveRequest.departmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + effectiveRequest.departmentId()));
                employee.setDepartment(department);
            }

            if (effectiveRequest.role() != null) {
                employee.setRole(effectiveRequest.role());
            }

            if (effectiveRequest.timezone() != null) {
                if (effectiveRequest.timezone().trim().isBlank()) {
                    throw new BadRequestException("Timezone cannot be blank");
                }
                employee.setTimezone(effectiveRequest.timezone().trim());
            }

            if (effectiveRequest.managerId() != null) {
                DepartmentManager manager = departmentManagerRepository.findById(effectiveRequest.managerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Department manager not found with id: " + effectiveRequest.managerId()));

                Long targetDeptId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;
                Long managerDeptId = (manager.getEmployee() != null && manager.getEmployee().getDepartment() != null)
                        ? manager.getEmployee().getDepartment().getId()
                        : null;

                if (targetDeptId == null || managerDeptId == null || !targetDeptId.equals(managerDeptId)) {
                    log.warn("Employee update rejected: managerId={} does not belong to departmentId={}", effectiveRequest.managerId(), targetDeptId);
                    throw new BadRequestException("Selected manager does not belong to the employee's department");
                }

                if (manager.getEmployee() != null && manager.getEmployee().getId().equals(employee.getId())) {
                    log.warn("Employee update rejected: manager cannot be assigned to themselves for employeeId={}", employee.getId());
                    throw new BadRequestException("Manager cannot be assigned to themselves as their own manager");
                }

                employee.setManager(manager);
            }
        }

        // 7. Handle Profile Image
        if (profileImage != null && !profileImage.isEmpty()) {
            ticketAttachmentValidator.validate(List.of(profileImage));
            if (profileImage.getContentType() == null || !profileImage.getContentType().startsWith("image/")) {
                throw new BadRequestException("Profile image must be an image (JPEG or PNG)");
            }

            byte[] fileData = profileImage.getBytes();

            TicketAttachment attachment = ticketAttachmentRepository
                    .findByEmployeeIdAndAttachmentType(employee.getId(), AttachmentType.PROFILE_IMG)
                    .orElseGet(TicketAttachment::new);

            attachment.setEmployee(employee);
            attachment.setUploadedBy(currentEmployee);
            attachment.setAttachmentType(AttachmentType.PROFILE_IMG);
            attachment.setOriginalFilename(profileImage.getOriginalFilename() != null ? profileImage.getOriginalFilename() : "profile.jpg");
            attachment.setMimeType(profileImage.getContentType());
            attachment.setFileSize(profileImage.getSize());
            attachment.setFileData(fileData);

            ticketAttachmentRepository.save(attachment);
            log.info("Profile image updated for employeeId={} by uploadedBy={}", employee.getId(), currentEmployeeId);
        }

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee updated successfully: employeeId={}", savedEmployee.getId());

        return toResponse(savedEmployee);
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
                    "Unable to verify agent ticket status counts.");
        }
    }
}