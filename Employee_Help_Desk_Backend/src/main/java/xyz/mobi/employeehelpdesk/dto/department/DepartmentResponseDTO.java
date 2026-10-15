package xyz.mobi.employeehelpdesk.dto.department;

public record DepartmentResponseDTO(
        Long id,
        String code,
        String name,
        String description
) {
}
