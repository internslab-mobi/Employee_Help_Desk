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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationResponseDTO;
import xyz.mobi.employeehelpdesk.dto.notification.UnreadNotificationCountResponseDTO;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.service.NotificationService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper);

        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void testGetNotifications() throws Exception {
        NotificationResponseDTO dto = new NotificationResponseDTO(
                1L, 10L, 101L, "TKT-001",
                NotificationType.TICKET_CREATED, "Ticket Created",
                "Your ticket TKT-001 has been logged", false, Instant.now(), Instant.now()
        );
        when(notificationService.getNotifications(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/helpdesk/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Ticket Created"))
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    @Test
    void testGetUnreadNotifications() throws Exception {
        NotificationResponseDTO dto = new NotificationResponseDTO(
                2L, 10L, 102L, "TKT-002",
                NotificationType.TICKET_ASSIGNED, "Ticket Assigned",
                "Ticket assigned to support agent", false, Instant.now(), Instant.now()
        );
        when(notificationService.getUnreadNotifications(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/helpdesk/notifications/unread")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2L))
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    @Test
    void testGetUnreadCount() throws Exception {
        when(notificationService.getUnreadCount())
                .thenReturn(new UnreadNotificationCountResponseDTO(5L));

        mockMvc.perform(get("/helpdesk/notifications/unread/count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void testMarkAsRead() throws Exception {
        when(notificationService.markAsRead(eq(1L)))
                .thenReturn(new NotificationResponseDTO(
                        1L, 10L, 101L, "TKT-001",
                        NotificationType.TICKET_CREATED, "Ticket Created",
                        "Your ticket has been logged", true, Instant.now(), Instant.now()
                ));

        mockMvc.perform(patch("/helpdesk/notifications/1/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.read").value(true));
    }
}
