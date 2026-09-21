package xyz.mobi.employeehelpdesk.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;

import java.util.List;
import java.util.Set;

@Component
public class TicketAttachmentValidator {

    private static final int MAX_FILES = 5;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB

    private static final long MAX_TOTAL_SIZE = 25 * 1024 * 1024L; // 25 MB

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf",
            "text/plain"
    );


    public void validate(List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return;
        }

        if (files.size() > MAX_FILES) {
            throw new BadRequestException(
                    "Maximum " + MAX_FILES + " attachments are allowed"
            );
        }

        long totalSize = 0;

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                continue;
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new BadRequestException(
                        "File exceeds maximum size of 10 MB: "+ file.getOriginalFilename()
                );
            }

            totalSize += file.getSize();

            if (file.getContentType() == null ||
                    !ALLOWED_TYPES.contains(file.getContentType())) {

                throw new BadRequestException(
                        "Unsupported file type: "+ file.getOriginalFilename()
                );
            }
        }

        if (totalSize > MAX_TOTAL_SIZE) {
            throw new BadRequestException(
                    "Total attachment size cannot exceed 25 MB"
            );
        }
    }
}