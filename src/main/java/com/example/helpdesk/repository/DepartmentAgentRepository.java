package com.example.helpdesk.repository;

import com.example.helpdesk.entity.DepartmentAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentAgentRepository extends JpaRepository<DepartmentAgent, Long> {
    List<DepartmentAgent> findByDepartmentId(Long departmentId);
    Optional<DepartmentAgent> findByEmployeeId(Long employeeId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedAgent.id = :agentId AND t.status NOT IN :statuses")
    long countByIdAndTicketStatusNotIn(@Param("agentId") Long agentId, @Param("statuses") List<String> statuses);
}



