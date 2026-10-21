package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.holiday.CreateHolidayRequestDTO;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayResponseDTO;

import java.util.List;

public interface HolidayService {

    List<HolidayResponseDTO> getHolidays(Long departmentId, Integer year);

    HolidayResponseDTO createHoliday(CreateHolidayRequestDTO request);

    void deleteHoliday(Long id);
}
