package com.digitalcafe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    // Password removed validation — user sets later
    private String password;

    @NotBlank(message = "Role is required")
    private String role; // ADMIN, CAFE_OWNER, CHEF, WAITER, CUSTOMER

    // Optional during initial registration
    @Valid
    private PersonalDetailsRequest personalDetails;

    @Valid
    private AddressRequest address;

    @Valid
    private List<AcademicInfoRequest> academicInfoList;

    @Valid
    private List<WorkExperienceRequest> workExperienceList; // optional
}
