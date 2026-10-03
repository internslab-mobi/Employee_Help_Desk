package com.example.helpdesk.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class TimezoneUtil {

    private static final ZoneId UTC = ZoneOffset.UTC;

    /**
     * Converts an Instant to OffsetDateTime using the specified timezone.
     * If the timezone is null or invalid, falls back to UTC.
     *
     * @param instant   the instant to convert
     * @param timezone  the IANA timezone ID (e.g., "Asia/Kolkata")
     * @return OffsetDateTime in the specified timezone, or UTC if timezone is invalid
     */
    public static OffsetDateTime toOffsetDateTime(Instant instant, String timezone) {
        if (instant == null) {
            return null;
        }

        ZoneId zoneId = parseZoneId(timezone);
        return instant.atZone(zoneId).toOffsetDateTime();
    }

    /**
     * Parses a timezone string into a ZoneId.
     * Returns UTC if the timezone is null or invalid.
     *
     * @param timezone the IANA timezone ID
     * @return ZoneId, or UTC if invalid
     */
    public static ZoneId parseZoneId(String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            return UTC;
        }

        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return UTC;
        }
    }

    /**
     * Gets the UTC ZoneId.
     *
     * @return ZoneId for UTC
     */
    public static ZoneId getUtcZoneId() {
        return UTC;
    }
}



