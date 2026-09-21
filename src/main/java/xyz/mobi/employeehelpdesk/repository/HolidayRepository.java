package xyz.mobi.employeehelpdesk.repository;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Holiday;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {

    List<Holiday> findByHolidayDateBetween(
            LocalDate from,
            LocalDate to
    );

    boolean existsByHolidayDate(@NotNull(message = "Holiday date is required") LocalDate localDate);
}
