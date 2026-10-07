package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.Holiday;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    @Query("""
        SELECT h
        FROM Holiday h
        WHERE h.department.id = :departmentId
          AND h.holidayDate BETWEEN :from AND :to
    """)
    List<Holiday> findByDepartmentIdAndHolidayDateBetween(
            @Param("departmentId") Long departmentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

}

