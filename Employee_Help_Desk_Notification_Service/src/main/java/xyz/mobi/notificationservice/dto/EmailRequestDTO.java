package xyz.mobi.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmailRequestDTO(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient email must be a valid email address")
        @JsonAlias({"recipientEmail", "to", "email"})
        String to,

        @NotBlank(message = "Subject is required")
        @Size(max = 255, message = "Subject cannot exceed 255 characters")
        @JsonAlias({"subject", "title"})
        String subject,

        @NotBlank(message = "Message body is required")
        @JsonAlias({"body", "message", "content", "text"})
        String body
) {
}
