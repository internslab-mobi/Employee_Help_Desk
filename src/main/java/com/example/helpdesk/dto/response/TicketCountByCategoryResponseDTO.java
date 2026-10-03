package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCountByCategoryResponseDTO {

    private Long categoryId;
    private String categoryName;
    private Long count;
}




