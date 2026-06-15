package com.travel.planner.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email 為必填欄位")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank(message = "密碼為必填欄位")
    private String password;

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
}
