package xyz.mobi.employeehelpdesk.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.ZoneId;
import java.util.Set;

public class TimezoneValidator implements ConstraintValidator<ValidTimezone, String> {

    private boolean allowNull;
    private static final Set<String> AVAILABLE_ZONES = ZoneId.getAvailableZoneIds();

    @Override
    public void initialize(ValidTimezone constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return allowNull;
        }

        String trimmed = value.trim();

        // Reject deprecated/ambiguous 3-letter abbreviations like IST, EST, PST if not standard
        if (trimmed.equalsIgnoreCase("IST") || trimmed.equalsIgnoreCase("EST") || trimmed.equalsIgnoreCase("PST")) {
            return false;
        }

        try {
            ZoneId zoneId = ZoneId.of(trimmed);
            return AVAILABLE_ZONES.contains(zoneId.getId())
                    || "UTC".equals(zoneId.getId())
                    || "GMT".equals(zoneId.getId())
                    || "Z".equals(zoneId.getId());
        } catch (Exception e) {
            return false;
        }
    }
}
