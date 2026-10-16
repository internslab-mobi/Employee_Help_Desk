package xyz.mobi.employeehelpdesk.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.mobi.employeehelpdesk.dto.message.TicketAttachmentResponseDTO;
import xyz.mobi.employeehelpdesk.dto.ticket.*;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketView;
import xyz.mobi.employeehelpdesk.service.TicketService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TicketControllerTest {

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper);

        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void testGetTicket() throws Exception {
        TicketResponseDTO responseDTO = TicketResponseDTO.builder()
                .id(101L)
                .ticketNumber("TKT-20261010-001")
                .subject("Network Issue")
                .description("VPN disconnected")
                .status("OPEN")
                .priority("HIGH")
                .slaStatus(SlaStatus.ACTIVE)
                .reopenCount(0)
                .createdAt(Instant.now())
                .build();

        when(ticketService.getTicket(101L)).thenReturn(responseDTO);

        mockMvc.perform(get("/helpdesk/tickets/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.ticketNumber").value("TKT-20261010-001"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void testSearchTickets() throws Exception {
        TicketResponseDTO responseDTO = TicketResponseDTO.builder()
                .id(101L)
                .ticketNumber("TKT-20261010-001")
                .subject("Network Issue")
                .status("OPEN")
                .priority("HIGH")
                .build();

        when(ticketService.searchTickets(eq(TicketView.CREATED), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDTO), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/helpdesk/tickets/search")
                        .param("view", "CREATED")
                        .param("search", "Network")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(101L))
                .andExpect(jsonPath("$.content[0].ticketNumber").value("TKT-20261010-001"));
    }

    @Test
    void testGetTicketHistory() throws Exception {
        TicketHistoryResponseDTO historyDTO = new TicketHistoryResponseDTO(
                1L,
                xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType.STATUS_CHANGED,
                TicketStatus.OPEN,
                TicketStatus.IN_PROGRESS,
                Instant.now()
        );

        when(ticketService.getTicketHistory(101L)).thenReturn(List.of(historyDTO));

        mockMvc.perform(get("/helpdesk/tickets/101/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].eventType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[0].newValue").value("IN_PROGRESS"));
    }

    @Test
    void testGetTicketSla() throws Exception {
        TicketSlaResponseDTO slaDTO = new TicketSlaResponseDTO(
                1L,
                "High Priority SLA",
                1,
                240,
                Instant.now(),
                Instant.now().plusSeconds(14400),
                Instant.now().plusSeconds(14400),
                Instant.now().plusSeconds(10800),
                SlaStatus.ACTIVE,
                null,
                null
        );

        when(ticketService.getTicketSla(101L)).thenReturn(slaDTO);

        mockMvc.perform(get("/helpdesk/tickets/101/sla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.policyName").value("High Priority SLA"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.allocatedMinutes").value(240));
    }

    @Test
    void testGetTicketAttachments() throws Exception {
        TicketAttachmentResponseDTO attDTO = new TicketAttachmentResponseDTO(
                5L,
                "vpn_error.png",
                "image/png",
                2048L,
                AttachmentType.INITIAL_ATTACHMENT
        );

        when(ticketService.getTicketAttachments(101L)).thenReturn(List.of(attDTO));

        mockMvc.perform(get("/helpdesk/tickets/101/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5L))
                .andExpect(jsonPath("$[0].originalFilename").value("vpn_error.png"))
                .andExpect(jsonPath("$[0].mimeType").value("image/png"));
    }

    @Test
    void testCreateTicketMultipart() throws Exception {
        String jsonPayload = """
                {
                  "departmentId": 1,
                  "categoryId": 10,
                  "subCategoryId": 100,
                  "subject": "Laptop Screen Flicker",
                  "description": "Screen flickers continuously on battery power"
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                jsonPayload.getBytes()
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "attachments",
                "screenshot.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        TicketResponseDTO createResponse = TicketResponseDTO.builder()
                .id(202L)
                .ticketNumber("TKT-20261014-001")
                .subject("Laptop Screen Flicker")
                .description("Screen flickers continuously on battery power")
                .status("OPEN")
                .priority("MEDIUM")
                .createdAt(Instant.now())
                .build();

        when(ticketService.createTicket(any(CreateTicketRequestDTO.class), any()))
                .thenReturn(createResponse);

        mockMvc.perform(multipart("/helpdesk/tickets")
                        .file(requestPart)
                        .file(filePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(202L))
                .andExpect(jsonPath("$.ticketNumber").value("TKT-20261014-001"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }
}
