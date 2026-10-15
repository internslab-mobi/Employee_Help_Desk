package xyz.mobi.employeehelpdesk.dto.department;

public record CategoryResponseDTO(
        Long id,
        Long departmentId,
        String name,
        String description
) {
}
