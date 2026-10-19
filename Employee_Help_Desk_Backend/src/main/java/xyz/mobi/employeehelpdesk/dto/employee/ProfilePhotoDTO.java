package xyz.mobi.employeehelpdesk.dto.employee;

public record ProfilePhotoDTO(
        byte[] data,
        String mimeType,
        String filename
) {
}
