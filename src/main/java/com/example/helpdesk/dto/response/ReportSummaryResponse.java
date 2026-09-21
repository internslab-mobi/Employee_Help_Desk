package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {

    private Long totalTickets;
    private Long openTickets;
    private Long resolvedTickets;
    private Long breachedTickets;
    private Long reopenedTickets;
    private Long slaMetCount;
    private Long slaBreachedCount;
    private Long slaWarningCount;
    private Double averageResolutionTime;
    private Double averageSlaResolutionTime;
    private Double averageFeedbackRating;
    private Long totalFeedbackCount;
}
