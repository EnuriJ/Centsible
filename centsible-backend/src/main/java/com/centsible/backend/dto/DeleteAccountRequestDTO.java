package com.centsible.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class DeleteAccountRequestDTO {

    @NotBlank(message = "Password is required")
    private String password;

    public DeleteAccountRequestDTO() {
    }

    public DeleteAccountRequestDTO(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
