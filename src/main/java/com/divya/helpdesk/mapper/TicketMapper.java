package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.ticket.AgentDTO;
import com.divya.helpdesk.dto.ticket.CategoryDTO;
import com.divya.helpdesk.dto.ticket.SubCategoryDTO;
import com.divya.helpdesk.dto.ticket.TicketResponse;
import com.divya.helpdesk.entity.HDCategory;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDSubCategory;
import com.divya.helpdesk.entity.HDTicket;
import lombok.Builder;

@Builder
public class TicketMapper {

    public static TicketResponse mapToResponse(HDTicket ticket, boolean includeUpdatedAt) {
        if (ticket == null) {
            return null;
        }

        return TicketResponse.builder()
                .ticketId(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .requester(toEmployeeResponse(ticket.getRequester()))
                .department(EmployeeMapper.toDepartmentResponse(ticket.getDepartment()))
                .category(toCategoryResponse(ticket.getCategory()))
                .subCategory(toSubCategoryResponse(ticket.getSubCategory()))
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .agent(toEmployeeResponse(ticket.getAssignedAgent()))
                .manager(toEmployeeResponse(ticket.getAssignedManager()))
                .reopenCount(ticket.getReopenCount())
                .resolutionSummary(ticket.getResolutionSummary())
                .withdrawalReason(ticket.getWithdrawalReason())
                .resolvedAt(ticket.getResolvedAt())
                .withdrawnAt(ticket.getWithdrawnAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(includeUpdatedAt ? ticket.getUpdatedAt() : null)
                .build();
    }

    public static AgentDTO toEmployeeResponse(HDEmployee employee){
        if(employee == null) return null;

        return AgentDTO.builder()
                .id(employee.getId())
                .name(employee.getFirstName() + " " + employee.getLastName())
                .build();
    }

    public static CategoryDTO toCategoryResponse(HDCategory category){
        if(category == null) return null;

        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static SubCategoryDTO toSubCategoryResponse(HDSubCategory subCategory){
        if(subCategory == null) return null;

        return SubCategoryDTO.builder()
                .id(subCategory.getId())
                .name(subCategory.getName())
                .build();
    }
}
