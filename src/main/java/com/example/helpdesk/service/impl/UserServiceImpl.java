package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.CreateEmployeeRequestDTO;
import com.example.helpdesk.dto.request.CreateUserRequestDTO;
import com.example.helpdesk.dto.response.EmployeeResponseDTO;
import com.example.helpdesk.entity.AgentSkill;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.DepartmentAgent;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Skill;
import com.example.helpdesk.entity.SubCategory;
import com.example.helpdesk.enums.Role;
import com.example.helpdesk.exception.ResourceNotFoundException;
import com.example.helpdesk.repository.AgentSkillRepository;
import com.example.helpdesk.repository.CategoryRepository;
import com.example.helpdesk.repository.DepartmentAgentRepository;
import com.example.helpdesk.repository.DepartmentRepository;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.repository.SkillRepository;
import com.example.helpdesk.repository.SubCategoryRepository;
import com.example.helpdesk.service.EmailService;
import com.example.helpdesk.service.EmployeeCodeGenerator;
import com.example.helpdesk.service.OTPService;
import com.example.helpdesk.service.PasswordGenerationService;
import com.example.helpdesk.service.UserService;
import com.example.helpdesk.util.AuthenticatedEmployeeUtil;
import com.example.helpdesk.util.TimezoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SkillRepository skillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerationService passwordGenerationService;
    private final AuthenticatedEmployeeUtil authenticatedEmployeeUtil;
    private final OTPService otpService;
    private final EmailService emailService;
    private final EmployeeCodeGenerator employeeCodeGenerator;

    @Value("${app.security.otp-expiration-minutes:10}")
    private int otpExpirationMinutes;

    // ==================== EMPLOYEES ====================

    @Override
    @Transactional
    public EmployeeResponseDTO createEmployee(CreateEmployeeRequestDTO request) {
        log.info("Creating employee with email: {}", request.getEmail());

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        String temporaryPassword = passwordGenerationService.generateTemporaryPassword();
        String temporaryPasswordHash = passwordEncoder.encode(temporaryPassword);

        TimezoneUtil.parseZoneId(request.getTimezone());

        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .designation(request.getDesignation())
                .department(department)
                .employmentStatus(request.getEmploymentStatus())
                .dateOfJoining(request.getDateOfJoining())
                .dateOfExit(null)
                .passwordHash(temporaryPasswordHash)
                .mustChangePassword(true)
                .role(request.getRole())
                .timezone(request.getTimezone())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);

        // Generate employeeCode based on the auto-generated ID
        String employeeCode = employeeCodeGenerator.generateEmployeeCode(savedEmployee.getId());
        savedEmployee.setEmployeeCode(employeeCode);
        savedEmployee = employeeRepository.save(savedEmployee);

        String otp = otpService.createAndStoreOTP(savedEmployee);

        emailService.sendAccountCreatedEmail(savedEmployee, temporaryPassword, otp, otpExpirationMinutes);

        log.info("Employee created with email {} and first-login credentials sent", savedEmployee.getEmail());

        return mapToEmployeeResponseDTO(savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO createUser(CreateUserRequestDTO request) {
        log.info("Creating user with type: {} and email: {}", request.getType(), request.getEmail());

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        String temporaryPassword = passwordGenerationService.generateTemporaryPassword();
        String temporaryPasswordHash = passwordEncoder.encode(temporaryPassword);

        TimezoneUtil.parseZoneId(request.getTimezone());

        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .designation(request.getDesignation())
                .department(department)
                .employmentStatus(request.getEmploymentStatus())
                .dateOfJoining(request.getDateOfJoining())
                .dateOfExit(null)
                .passwordHash(temporaryPasswordHash)
                .mustChangePassword(true)
                .role(request.getType())
                .timezone(request.getTimezone())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);

        // Generate employeeCode based on the auto-generated ID
        String employeeCode = employeeCodeGenerator.generateEmployeeCode(savedEmployee.getId());
        savedEmployee.setEmployeeCode(employeeCode);
        savedEmployee = employeeRepository.save(savedEmployee);

        String otp = otpService.createAndStoreOTP(savedEmployee);

        emailService.sendAccountCreatedEmail(savedEmployee, temporaryPassword, otp, otpExpirationMinutes);

        log.info("User created with type {} and email {} and first-login credentials sent", request.getType(), savedEmployee.getEmail());

        return mapToEmployeeResponseDTO(savedEmployee);
    }

    @Override
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    @Override
    public Optional<Employee> getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    @Transactional
    public Employee updateEmployee(Long id, Employee employee) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        
        existing.setFirstName(employee.getFirstName());
        existing.setLastName(employee.getLastName());
        existing.setEmail(employee.getEmail());
        existing.setPhone(employee.getPhone());
        existing.setDesignation(employee.getDesignation());
        existing.setDepartment(employee.getDepartment());
        existing.setEmploymentStatus(employee.getEmploymentStatus());
        existing.setDateOfJoining(employee.getDateOfJoining());
        existing.setDateOfExit(employee.getDateOfExit());
        existing.setProfileImage(employee.getProfileImage());
        existing.setProfileImageType(employee.getProfileImageType());
        existing.setRole(employee.getRole());
        
        return employeeRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
    }

    @Override
    public List<Employee> getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    @Override
    public List<Employee> getEmployeesByRole(String role) {
        return employeeRepository.findByRole(role);
    }

    // ==================== AGENTS ====================

    @Override
    @Transactional
    public EmployeeResponseDTO createAgent(CreateEmployeeRequestDTO request) {
        log.info("Creating agent with email: {}", request.getEmail());

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        String temporaryPassword = passwordGenerationService.generateTemporaryPassword();
        String temporaryPasswordHash = passwordEncoder.encode(temporaryPassword);

        TimezoneUtil.parseZoneId(request.getTimezone());

        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .designation(request.getDesignation())
                .department(department)
                .employmentStatus(request.getEmploymentStatus())
                .dateOfJoining(request.getDateOfJoining())
                .dateOfExit(null)
                .passwordHash(temporaryPasswordHash)
                .mustChangePassword(true)
                .role(Role.AGENT)
                .timezone(request.getTimezone())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);

        // Generate employeeCode based on the auto-generated ID
        String employeeCode = employeeCodeGenerator.generateEmployeeCode(savedEmployee.getId());
        savedEmployee.setEmployeeCode(employeeCode);
        savedEmployee = employeeRepository.save(savedEmployee);

        String otp = otpService.createAndStoreOTP(savedEmployee);

        emailService.sendAccountCreatedEmail(savedEmployee, temporaryPassword, otp, otpExpirationMinutes);

        log.info("Agent created with email {} and first-login credentials sent", savedEmployee.getEmail());

        return mapToEmployeeResponseDTO(savedEmployee);
    }

    @Override
    public List<Employee> getAllAgents() {
        return employeeRepository.findByRole(Role.AGENT.name());
    }

    // ==================== MANAGERS ====================

    @Override
    @Transactional
    public EmployeeResponseDTO createManager(CreateEmployeeRequestDTO request) {
        log.info("Creating manager with email: {}", request.getEmail());

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        String temporaryPassword = passwordGenerationService.generateTemporaryPassword();
        String temporaryPasswordHash = passwordEncoder.encode(temporaryPassword);

        TimezoneUtil.parseZoneId(request.getTimezone());

        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .designation(request.getDesignation())
                .department(department)
                .employmentStatus(request.getEmploymentStatus())
                .dateOfJoining(request.getDateOfJoining())
                .dateOfExit(null)
                .passwordHash(temporaryPasswordHash)
                .mustChangePassword(true)
                .role(Role.MANAGER)
                .timezone(request.getTimezone())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);

        // Generate employeeCode based on the auto-generated ID
        String employeeCode = employeeCodeGenerator.generateEmployeeCode(savedEmployee.getId());
        savedEmployee.setEmployeeCode(employeeCode);
        savedEmployee = employeeRepository.save(savedEmployee);

        String otp = otpService.createAndStoreOTP(savedEmployee);

        emailService.sendAccountCreatedEmail(savedEmployee, temporaryPassword, otp, otpExpirationMinutes);

        log.info("Manager created with email {} and first-login credentials sent", savedEmployee.getEmail());

        return mapToEmployeeResponseDTO(savedEmployee);
    }

    @Override
    public List<Employee> getAllManagers() {
        return employeeRepository.findByRole(Role.MANAGER.name());
    }

    // ==================== DEPARTMENTS ====================

    @Override
    @Transactional
    public Department createDepartment(Department department) {
        log.info("Creating department with code: {}", department.getCode());
        return departmentRepository.save(department);
    }

    @Override
    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    @Transactional
    public Department updateDepartment(Long id, Department department) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        
        existing.setCode(department.getCode());
        existing.setName(department.getName());
        existing.setDescription(department.getDescription());
        existing.setActive(department.getActive());
        
        return departmentRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void activateDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        department.setActive(true);
        departmentRepository.save(department);
    }

    @Override
    @Transactional
    public void deactivateDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        department.setActive(false);
        departmentRepository.save(department);
    }

    // ==================== CATEGORIES ====================

    @Override
    @Transactional
    public Category createCategory(Category category) {
        log.info("Creating category: {}", category.getName());
        return categoryRepository.save(category);
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public List<Category> getCategoriesByDepartment(Long departmentId) {
        return categoryRepository.findByDepartmentId(departmentId);
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        existing.setDepartment(category.getDepartment());
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setActive(category.getActive());
        existing.setCreatedBy(category.getCreatedBy());
        
        return categoryRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void activateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setActive(true);
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setActive(false);
        categoryRepository.save(category);
    }

    // ==================== SUB-CATEGORIES ====================

    @Override
    @Transactional
    public SubCategory createSubCategory(SubCategory subCategory) {
        log.info("Creating sub-category: {}", subCategory.getName());
        return subCategoryRepository.save(subCategory);
    }

    @Override
    public Optional<SubCategory> getSubCategoryById(Long id) {
        return subCategoryRepository.findById(id);
    }

    @Override
    public List<SubCategory> getAllSubCategories() {
        return subCategoryRepository.findAll();
    }

    @Override
    public List<SubCategory> getSubCategoriesByCategory(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId);
    }

    @Override
    @Transactional
    public SubCategory updateSubCategory(Long id, SubCategory subCategory) {
        SubCategory existing = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        
        existing.setCategory(subCategory.getCategory());
        existing.setName(subCategory.getName());
        existing.setDescription(subCategory.getDescription());
        existing.setActive(subCategory.getActive());
        existing.setCreatedBy(subCategory.getCreatedBy());
        existing.setPriority(subCategory.getPriority());
        
        return subCategoryRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteSubCategory(Long id) {
        if (!subCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("SubCategory not found with id: " + id);
        }
        subCategoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void activateSubCategory(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        subCategory.setActive(true);
        subCategoryRepository.save(subCategory);
    }

    @Override
    @Transactional
    public void deactivateSubCategory(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        subCategory.setActive(false);
        subCategoryRepository.save(subCategory);
    }

    // ==================== SKILLS ====================

    @Override
    @Transactional
    public Skill createSkill(Skill skill) {
        log.info("Creating skill: {}", skill.getName());
        return skillRepository.save(skill);
    }

    @Override
    public Optional<Skill> getSkillById(Long id) {
        return skillRepository.findById(id);
    }

    @Override
    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    @Override
    @Transactional
    public Skill updateSkill(Long id, Skill skill) {
        Skill existing = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
        
        existing.setName(skill.getName());
        existing.setDescription(skill.getDescription());
        existing.setActive(skill.getActive());
        
        return skillRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteSkill(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Skill not found with id: " + id);
        }
        skillRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void assignSkillToAgent(Long agentId, Long skillId) {
        DepartmentAgent departmentAgent = departmentAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department agent not found with id: " + agentId));
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + skillId));
        
        AgentSkill agentSkill = AgentSkill.builder()
                .agent(departmentAgent)
                .skill(skill)
                .build();
        
        agentSkillRepository.save(agentSkill);
        log.info("Assigned skill {} to agent {}", skill.getName(), departmentAgent.getEmployee().getEmployeeCode());
    }

    @Override
    @Transactional
    public void removeSkillFromAgent(Long agentId, Long skillId) {
        AgentSkill agentSkill = agentSkillRepository.findByAgentIdAndSkillId(agentId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent skill assignment not found"));
        
        agentSkillRepository.delete(agentSkill);
        log.info("Removed skill {} from agent {}", skillId, agentId);
    }

    private EmployeeResponseDTO mapToEmployeeResponseDTO(Employee employee) {
        EmployeeResponseDTO.DepartmentResponseDTO DepartmentResponseDTO = null;
        if (employee.getDepartment() != null) {
            DepartmentResponseDTO = EmployeeResponseDTO.DepartmentResponseDTO.builder()
                    .id(employee.getDepartment().getId())
                    .code(employee.getDepartment().getCode())
                    .name(employee.getDepartment().getName())
                    .description(employee.getDepartment().getDescription())
                    .active(employee.getDepartment().getActive())
                    .build();
        }

        return EmployeeResponseDTO.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .designation(employee.getDesignation())
                .department(DepartmentResponseDTO)
                .employmentStatus(employee.getEmploymentStatus())
                .dateOfJoining(employee.getDateOfJoining())
                .dateOfExit(employee.getDateOfExit())
                .role(employee.getRole())
                .timezone(employee.getTimezone())
                .createdAt(TimezoneUtil.toOffsetDateTime(employee.getCreatedAt(), authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone()))
                .build();
    }
}




