package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.agentskill.AgentSkillRequest;
import xyz.mobi.employeehelpdesk.dto.agentskill.AgentSkillResponse;
import xyz.mobi.employeehelpdesk.entity.AgentSkill;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.Skill;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.AgentSkillRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentAgentRepository;
import xyz.mobi.employeehelpdesk.repository.SkillRepository;
import xyz.mobi.employeehelpdesk.service.AgentSkillAdminService;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.DepartmentAuthorizationService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSkillAdminServiceImpl implements AgentSkillAdminService {

    private final DepartmentAgentRepository departmentAgentRepository;
    private final SkillRepository skillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public AgentSkillResponse assignSkillToAgent(Long agentId, AgentSkillRequest request) {
        DepartmentAgent agent = departmentAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("DepartmentAgent not found with id: " + agentId));

        checkDepartmentAccess(agent.getDepartment().getId());

        if (!Boolean.TRUE.equals(agent.getEmployee().getEnabled())) {
            throw new BadRequestException("Employee is disabled: " + agent.getEmployee().getEmail());
        }

        if (agent.getEmployee().getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BadRequestException("Employee is not active (status: " + agent.getEmployee().getEmploymentStatus() + ")");
        }

        if (agent.getEmployee().getRole() != UserRole.AGENT) {
            throw new BadRequestException("Employee does not have ROLE_AGENT (role: " + agent.getEmployee().getRole() + ")");
        }

        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + request.skillId()));

        if (!Boolean.TRUE.equals(skill.getIsActive())) {
            throw new BadRequestException("Skill is inactive: " + skill.getName());
        }

        if (agentSkillRepository.existsByAgentIdAndSkillId(agentId, request.skillId())) {
            throw new BadRequestException("Skill is already assigned to this agent: " + skill.getName());
        }

        AgentSkill agentSkill = new AgentSkill();
        agentSkill.setAgent(agent);
        agentSkill.setSkill(skill);

        AgentSkill saved = agentSkillRepository.save(agentSkill);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentSkillResponse> getAgentSkills(Long agentId) {
        DepartmentAgent agent = departmentAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("DepartmentAgent not found with id: " + agentId));

        checkDepartmentAccess(agent.getDepartment().getId());

        return agentSkillRepository.findByAgentId(agentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeSkillFromAgent(Long agentId, Long skillId) {
        DepartmentAgent agent = departmentAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("DepartmentAgent not found with id: " + agentId));

        checkDepartmentAccess(agent.getDepartment().getId());

        if (!skillRepository.existsById(skillId)) {
            throw new ResourceNotFoundException("Skill not found with id: " + skillId);
        }

        AgentSkill agentSkill = agentSkillRepository.findByAgentIdAndSkillId(agentId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill assignment not found for agent " + agentId + " and skill " + skillId));

        agentSkillRepository.delete(agentSkill);
    }

    private void checkDepartmentAccess(Long departmentId) {
        UserRole role = currentUserService.getCurrentUserRole();
        if (role == UserRole.ADMIN) {
            return;
        }
        if (role == UserRole.MANAGER) {
            Long currentEmpId = currentUserService.getCurrentEmployeeId();
            if (!departmentAuthorizationService.isManagerOfDepartment(currentEmpId, departmentId)) {
                throw new AccessDeniedException("Access denied: You are not a manager of department " + departmentId);
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient permissions");
    }

    private AgentSkillResponse toResponse(AgentSkill as) {
        String employeeName = as.getAgent().getEmployee().getFirstName()
                + (as.getAgent().getEmployee().getLastName() != null ? " " + as.getAgent().getEmployee().getLastName() : "");

        return AgentSkillResponse.builder()
                .id(as.getId())
                .agentId(as.getAgent().getId())
                .employeeId(as.getAgent().getEmployee().getId())
                .employeeName(employeeName)
                .departmentId(as.getAgent().getDepartment().getId())
                .departmentName(as.getAgent().getDepartment().getName())
                .skillId(as.getSkill().getId())
                .skillName(as.getSkill().getName())
                .createdAt(as.getCreatedAt())
                .build();
    }
}
