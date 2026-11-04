package com.radusclifos.questlog.user.service;

import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.dto.*;
import com.radusclifos.questlog.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

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

    public List<UserSuggestion> suggestions(String name, int limit) {
        if (name.strip().length() < 3 || name.length() > 200 || limit < 1 || limit > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use 3–200 characters and a limit from 1 to 20");
        }
        return users.suggestions(name.strip(), PageRequest.of(0, limit));
    }

    @Transactional
    public UserResponse update(Principal principal, UpdateProfileRequest request) {
        User user = current(principal);
        user.rename(request.getDisplayName());
        user.describe(request.getBio());
        return UserResponse.from(user);
    }

}
