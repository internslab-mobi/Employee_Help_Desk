package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(
        name = "error_codes",
        indexes = {
                @Index(name = "idx_error_codes_exception_type", columnList = "exception_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorCodeMapping extends BaseEntity {

    @Column(name = "exception_type", nullable = false, unique = true, length = 100)
    private String exceptionType;

    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;
}
