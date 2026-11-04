package com.radusclifos.questlog.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String displayName;

    @Size(max = 300)
    private String bio;

    public UpdateProfileRequest(String displayName) {
        this.displayName = displayName;
    }

}
