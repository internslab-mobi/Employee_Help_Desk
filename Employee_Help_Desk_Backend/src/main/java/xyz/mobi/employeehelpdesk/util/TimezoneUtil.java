package xyz.mobi.employeehelpdesk.util;

import java.time.ZoneId;
import java.util.Set;

public final class TimezoneUtil {

    private static final Set<String> AVAILABLE_ZONES =
            ZoneId.getAvailableZoneIds();

    private TimezoneUtil() {
    }

    public static boolean isValidTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return true; // Optional field
        }

        String trimmed = timezone.trim();

        // Accept any supported IANA region ID.
        return AVAILABLE_ZONES.contains(trimmed);
    }
}
