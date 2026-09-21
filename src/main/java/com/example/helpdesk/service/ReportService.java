package com.example.helpdesk.service;

import com.example.helpdesk.dto.response.*;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {

    ReportSummaryResponse getDashboardSummary();

    List<TicketCountByStatusResponse> getTicketCountByStatus();

    List<TicketCountByPriorityResponse> getTicketCountByPriority();

    List<TicketCountByDepartmentResponse> getTicketCountByDepartment();

    List<TicketCountByCategoryResponse> getTicketCountByCategory();

    List<TicketCountByAgentResponse> getTicketCountByAgent();

    List<TicketCountByCategoryResponse> getTicketCountBySubCategory();

    FeedbackSummaryResponse getFeedbackSummary();

    List<WorkloadResponse> getAgentWorkload();

    List<WorkloadResponse> getDepartmentWorkload();

    List<TicketCountByStatusResponse> getTicketVolumeByDate(LocalDateTime fromDate, LocalDateTime toDate);
}
