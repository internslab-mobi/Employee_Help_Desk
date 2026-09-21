package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.repository.DepartmentAgentRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentManagerRepository;
import xyz.mobi.employeehelpdesk.service.DepartmentAuthorizationService;

@Service
@RequiredArgsConstructor
public class DepartmentAuthorizationServiceImpl implements DepartmentAuthorizationService {

    private final DepartmentManagerRepository departmentManagerRepository;
    private final DepartmentAgentRepository departmentAgentRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isManagerOfDepartment(Long employeeId, Long departmentId) {
        if (employeeId == null || departmentId == null) {
            return false;
        }
        return departmentManagerRepository.existsByEmployeeIdAndDepartmentId(employeeId, departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAgentOfDepartment(Long employeeId, Long departmentId) {
        if (employeeId == null || departmentId == null) {
            return false;
        }
        return departmentAgentRepository.existsByEmployeeIdAndDepartmentId(employeeId, departmentId);
    }
}
