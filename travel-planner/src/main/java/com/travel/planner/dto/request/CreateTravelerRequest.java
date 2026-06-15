package com.travel.planner.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTravelerRequest {

    @NotBlank(message = "姓名為必填欄位")
    private String name;

    @NotBlank(message = "Email 為必填欄位")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank(message = "密碼為必填欄位")
    @Size(min = 6, message = "密碼至少需要 6 個字元")
    private String password;

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
}
