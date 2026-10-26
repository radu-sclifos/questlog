package com.radusclifos.questlog.user.controller;

import com.radusclifos.questlog.user.dto.*;
import com.radusclifos.questlog.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class UserController {

    private final UserService users;

    @Autowired
    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return UserResponse.from(users.current(principal));
    }

    @PatchMapping("/me")
    public UserResponse update(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return users.update(principal, request);
    }

    @GetMapping("/users/{username}")
    public UserResponse profile(Principal principal, @PathVariable String username) {
        users.current(principal);
        return UserResponse.from(users.find(username));
    }


}
