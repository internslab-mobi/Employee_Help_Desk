package xyz.mobi.employeehelpdesk.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.mobi.employeehelpdesk.dto.department.DepartmentResponseDTO;
import xyz.mobi.employeehelpdesk.service.DepartmentService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class DepartmentControllerTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private DepartmentController departmentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(departmentController).build();
    }

    @Test
    void testGetAllDepartments() throws Exception {
        DepartmentResponseDTO dto = DepartmentResponseDTO.builder()
                .id(1L)
                .code("IT")
                .name("Information Technology")
                .description("IT Dept")
                .isActive(true)
                .timezone("UTC")
                .build();

        when(departmentService.getAllDepartments()).thenReturn(List.of(dto));

        mockMvc.perform(get("/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Information Technology"))
                .andExpect(jsonPath("$[0].code").value("IT"));
    }

    @Test
    void testGetDepartmentById() throws Exception {
        DepartmentResponseDTO dto = DepartmentResponseDTO.builder()
                .id(1L)
                .code("IT")
                .name("Information Technology")
                .description("IT Dept")
                .isActive(true)
                .timezone("UTC")
                .build();

        when(departmentService.getDepartmentById(1L)).thenReturn(dto);

        mockMvc.perform(get("/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Information Technology"));
    }
}
