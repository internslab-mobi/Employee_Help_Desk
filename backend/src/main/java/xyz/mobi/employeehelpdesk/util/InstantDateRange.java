package xyz.mobi.employeehelpdesk.util;

import xyz.mobi.employeehelpdesk.exception.BadRequestException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/*
 * Reusable utility representing a half-open UTC Instant range [from, toExclusive)
 * converted from local calendar dates (LocalDate) in a user's specific IANA timezone.
 */
public record InstantDateRange(
        Instant from,
        Instant toExclusive
) {

    /*
     * Converts calendar dates to a half-open UTC Instant range.
     */
    public static InstantDateRange of(LocalDate fromDate, LocalDate toDate, ZoneId zoneId) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate cannot be after toDate");
        }

        ZoneId effectiveZone = zoneId != null ? zoneId : ZoneId.of("UTC");

        Instant fromInstant = fromDate != null
                ? fromDate.atStartOfDay(effectiveZone).toInstant()
                : null;

        Instant toInstantExclusive = toDate != null
                ? toDate.plusDays(1).atStartOfDay(effectiveZone).toInstant()
                : null;

        return new InstantDateRange(fromInstant, toInstantExclusive);
    }

    /*
     * Overload accepting an IANA timezone string.
     */
    public static InstantDateRange of(LocalDate fromDate, LocalDate toDate, String timezone) {
        ZoneId zoneId;
        try {
            zoneId = (timezone != null && !timezone.isBlank())
                    ? ZoneId.of(timezone.trim())
                    : ZoneId.of("UTC");
        } catch (Exception e) {
            zoneId = ZoneId.of("UTC");
        }
        return of(fromDate, toDate, zoneId);
    }
}
