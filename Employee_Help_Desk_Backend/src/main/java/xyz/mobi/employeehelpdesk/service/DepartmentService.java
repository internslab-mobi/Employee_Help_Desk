package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.department.DepartmentResponseDTO;

import java.util.List;

public interface DepartmentService {

    List<DepartmentResponseDTO> getAllDepartments();

    DepartmentResponseDTO getDepartmentById(Long id);
}
