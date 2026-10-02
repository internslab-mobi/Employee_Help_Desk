package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.user.CreateEmployeeResponseDTO;
import com.divya.helpdesk.dto.user.DepartmentDTO;
import com.divya.helpdesk.dto.user.UpdateEmployeeResponseDTO;
import com.divya.helpdesk.entity.HDDepartmentEntity;
import com.divya.helpdesk.entity.HDEmployeeEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "department", source = "department")
    CreateEmployeeResponseDTO mapToResponse(HDEmployeeEntity employee);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "department", source = "department")
    UpdateEmployeeResponseDTO mapToUpdateResponse(HDEmployeeEntity employee);

    DepartmentDTO toDepartmentResponse(HDDepartmentEntity department);
}
