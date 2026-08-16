package com.friendchat.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be 2–100 characters")
    private String name;

    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    @Pattern(regexp = "^[a-z0-9_]+$",
             message = "Username may only contain lowercase letters, numbers and underscores")
    private String username;

    @Size(max = 160, message = "Bio may not exceed 160 characters")
    private String bio;
}
