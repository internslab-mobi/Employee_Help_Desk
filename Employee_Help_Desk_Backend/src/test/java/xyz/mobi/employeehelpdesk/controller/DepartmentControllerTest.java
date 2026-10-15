package xyz.mobi.employeehelpdesk.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.mobi.employeehelpdesk.entity.Category;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.SubCategory;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.repository.CategoryRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategoryRepository;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class DepartmentControllerTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubCategoryRepository subCategoryRepository;

    @InjectMocks
    private DepartmentController departmentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(departmentController).build();
    }

    @Test
    void testGetActiveDepartments() throws Exception {
        Department d = new Department();
        d.setId(1L);
        d.setCode("IT");
        d.setName("Information Technology");
        d.setDescription("IT Dept");

        when(departmentRepository.findByIsActiveTrue()).thenReturn(List.of(d));

        mockMvc.perform(get("/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Information Technology"));
    }

    @Test
    void testGetCategoriesByDepartment() throws Exception {
        Department d = new Department();
        d.setId(1L);

        Category c = new Category();
        c.setId(10L);
        c.setDepartment(d);
        c.setName("Hardware");
        c.setDescription("Hardware issues");

        when(categoryRepository.findByDepartmentIdAndIsActiveTrue(1L)).thenReturn(List.of(c));

        mockMvc.perform(get("/departments/1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].name").value("Hardware"));
    }

    @Test
    void testGetSubCategoriesByCategory() throws Exception {
        Category c = new Category();
        c.setId(10L);

        SubCategory sc = new SubCategory();
        sc.setId(100L);
        sc.setCategory(c);
        sc.setName("Laptop Issue");
        sc.setDescription("Laptop repair");
        sc.setPriority(Priority.HIGH);

        when(subCategoryRepository.findByCategoryIdAndIsActiveTrue(10L)).thenReturn(List.of(sc));

        mockMvc.perform(get("/categories/10/subcategories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].name").value("Laptop Issue"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));
    }
}
