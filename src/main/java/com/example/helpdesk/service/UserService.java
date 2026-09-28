package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.CreateEmployeeRequest;
import com.example.helpdesk.dto.request.CreateUserRequest;
import com.example.helpdesk.dto.response.EmployeeResponse;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Skill;
import com.example.helpdesk.entity.SubCategory;

import java.util.List;
import java.util.Optional;

public interface UserService {

    // ==================== EMPLOYEES ====================

    EmployeeResponse createEmployee(CreateEmployeeRequest request);

    EmployeeResponse createUser(CreateUserRequest request);

    Optional<Employee> getEmployeeById(Long id);

    Optional<Employee> getEmployeeByEmail(String email);

    List<Employee> getAllEmployees();

    Employee updateEmployee(Long id, Employee employee);

    void deleteEmployee(Long id);

    List<Employee> getEmployeesByDepartment(Long departmentId);

    List<Employee> getEmployeesByRole(String role);

    // ==================== AGENTS ====================

    EmployeeResponse createAgent(CreateEmployeeRequest request);

    List<Employee> getAllAgents();

    // ==================== MANAGERS ====================

    EmployeeResponse createManager(CreateEmployeeRequest request);

    List<Employee> getAllManagers();

    // ==================== DEPARTMENTS ====================

    Department createDepartment(Department department);

    Optional<Department> getDepartmentById(Long id);

    List<Department> getAllDepartments();

    Department updateDepartment(Long id, Department department);

    void deleteDepartment(Long id);

    void activateDepartment(Long id);

    void deactivateDepartment(Long id);

    // ==================== CATEGORIES ====================

    Category createCategory(Category category);

    Optional<Category> getCategoryById(Long id);

    List<Category> getAllCategories();

    List<Category> getCategoriesByDepartment(Long departmentId);

    Category updateCategory(Long id, Category category);

    void deleteCategory(Long id);

    void activateCategory(Long id);

    void deactivateCategory(Long id);

    // ==================== SUB-CATEGORIES ====================

    SubCategory createSubCategory(SubCategory subCategory);

    Optional<SubCategory> getSubCategoryById(Long id);

    List<SubCategory> getAllSubCategories();

    List<SubCategory> getSubCategoriesByCategory(Long categoryId);

    SubCategory updateSubCategory(Long id, SubCategory subCategory);

    void deleteSubCategory(Long id);

    void activateSubCategory(Long id);

    void deactivateSubCategory(Long id);

    // ==================== SKILLS ====================

    Skill createSkill(Skill skill);

    Optional<Skill> getSkillById(Long id);

    List<Skill> getAllSkills();

    Skill updateSkill(Long id, Skill skill);

    void deleteSkill(Long id);

    void assignSkillToAgent(Long agentId, Long skillId);

    void removeSkillFromAgent(Long agentId, Long skillId);

}
