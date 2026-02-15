package com.digitalcafe.dto.request;

import lombok.Data;

@Data
public class SetPasswordRequest {
    private String token;
    private String password;
}
