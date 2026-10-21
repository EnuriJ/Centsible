package com.centsible.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignUpRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    // BCrypt only uses the first 72 bytes of a password, so longer input would be silently
    // truncated (or rejected, depending on the library version) - cap it up front.
    @Size(min = 10, max = 72, message = "Password must be between 10 and 72 characters")
    private String password;

    private String displayName;

    public SignUpRequestDTO() {
    }

    public SignUpRequestDTO(String email, String password, String displayName) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
