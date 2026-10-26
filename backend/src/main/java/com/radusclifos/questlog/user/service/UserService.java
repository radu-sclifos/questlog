package com.radusclifos.questlog.user.service;

import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.dto.*;
import com.radusclifos.questlog.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;

    @Autowired
    public UserService(UserRepository users) {
        this.users = users;
    }

    public User current(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.findByUsername(principal.getName()).filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public User find(String username) {
        return users.findByUsername(username).filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserResponse update(Principal principal, UpdateProfileRequest request) {
        User user = current(principal);
        user.rename(request.getDisplayName());
        return UserResponse.from(user);
    }

}
