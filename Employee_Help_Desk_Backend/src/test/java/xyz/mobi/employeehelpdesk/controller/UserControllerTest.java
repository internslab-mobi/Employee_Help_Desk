package xyz.mobi.employeehelpdesk.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.UpdateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.service.EmployeeService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper);

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void testGetCurrentUserProfile() throws Exception {
        EmployeeResponseDTO profile = EmployeeResponseDTO.builder()
                .id(1L)
                .employeeCode("EMP001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@company.com")
                .phone("9876543210")
                .designation("Software Engineer")
                .departmentId(1L)
                .departmentName("Information Technology")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .role(UserRole.EMPLOYEE)
                .dateOfJoining(LocalDate.of(2023, 1, 15))
                .timezone("Asia/Kolkata")
                .build();

        when(employeeService.getCurrentEmployeeProfile(any())).thenReturn(profile);

        mockMvc.perform(get("/helpdesk/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@company.com"))
                .andExpect(jsonPath("$.phone").value("9876543210"))
                .andExpect(jsonPath("$.designation").value("Software Engineer"))
                .andExpect(jsonPath("$.departmentName").value("Information Technology"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void testUpdateCurrentUserProfile() throws Exception {
        String jsonPayload = """
                {
                  "firstName": "Johnny",
                  "lastName": "Doe",
                  "phone": "9988776655",
                  "designation": "Senior Software Engineer"
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                jsonPayload.getBytes()
        );

        EmployeeResponseDTO updated = EmployeeResponseDTO.builder()
                .id(1L)
                .employeeCode("EMP001")
                .firstName("Johnny")
                .lastName("Doe")
                .email("john.doe@company.com")
                .phone("9988776655")
                .designation("Senior Software Engineer")
                .departmentId(1L)
                .departmentName("Information Technology")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .role(UserRole.EMPLOYEE)
                .build();

        when(employeeService.updateEmployee(isNull(), any(UpdateEmployeeRequestDTO.class), isNull(), any()))
                .thenReturn(updated);

        mockMvc.perform(multipart(HttpMethod.PATCH, "/helpdesk/user/me")
                        .file(requestPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.phone").value("9988776655"))
                .andExpect(jsonPath("$.designation").value("Senior Software Engineer"));
    }

    @Test
    void testGetEmployeeById() throws Exception {
        EmployeeResponseDTO profile = EmployeeResponseDTO.builder()
                .id(2L)
                .employeeCode("EMP002")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@company.com")
                .role(UserRole.AGENT)
                .build();

        when(employeeService.getEmployeeById(org.mockito.ArgumentMatchers.eq(2L), any())).thenReturn(profile);

        mockMvc.perform(get("/helpdesk/user/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.employeeCode").value("EMP002"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }
}
