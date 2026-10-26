package com.radusclifos.questlog.user.dto;

import com.radusclifos.questlog.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String username;
    private String displayName;

    public static UserResponse from(User user) {
        return new UserResponse(user.getUsername(), user.getDisplayName());
    }

}
