package com.divya.helpdesk.util;

import com.divya.helpdesk.exception.BadRequestException;

import java.time.DateTimeException;
import java.time.ZoneId;

public final class TimezoneUtil {

    public static final String DEFAULT_TIMEZONE = "Asia/Kolkata";

    private TimezoneUtil() {
    }

    public static ZoneId validateAndGetZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new BadRequestException("Timezone cannot be blank");
        }
        String trimmed = timezone.trim();
        if (trimmed.startsWith("GMT+") || trimmed.startsWith("GMT-")
                || trimmed.startsWith("UTC+") || trimmed.startsWith("UTC-")
                || trimmed.equalsIgnoreCase("IST")) {
            throw new BadRequestException("Invalid timezone format: '" + timezone
                    + "'. Please use IANA timezone identifiers (e.g. Asia/Kolkata, Asia/Kuala_Lumpur, America/New_York)");
        }
        try {
            return ZoneId.of(trimmed);
        } catch (DateTimeException e) {
            throw new BadRequestException("Invalid timezone: '" + timezone
                    + "'. Must be a valid IANA timezone identifier (e.g. Asia/Kolkata, Asia/Kuala_Lumpur, America/New_York)");
        }
    }

    public static boolean isValidTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return false;
        }
        String trimmed = timezone.trim();
        if (trimmed.startsWith("GMT+") || trimmed.startsWith("GMT-")
                || trimmed.startsWith("UTC+") || trimmed.startsWith("UTC-")
                || trimmed.equalsIgnoreCase("IST")) {
            return false;
        }
        try {
            ZoneId.of(trimmed);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
