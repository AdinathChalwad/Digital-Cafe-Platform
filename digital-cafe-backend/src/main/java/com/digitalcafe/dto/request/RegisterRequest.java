package com.digitalcafe.dto.request;

import com.digitalcafe.dto.request.AcademicInfoRequest;
import com.digitalcafe.dto.request.AddressRequest;
import com.digitalcafe.dto.request.PersonalDetailsRequest;
import com.digitalcafe.dto.request.WorkExperienceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RegisterRequest {

    @NotNull(message = "Role is required")
    private String role;

    @Valid
    @NotNull(message = "Personal details are required")
    private PersonalDetailsRequest personalDetails;

    @Valid
    @NotNull(message = "Address is required")
    private AddressRequest address;

    @Valid
    @NotNull(message = "Academic information is required")
    private List<AcademicInfoRequest> academicInfoList;

    private List<WorkExperienceRequest> workExperienceList;
}
