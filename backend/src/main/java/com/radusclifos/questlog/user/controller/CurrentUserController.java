package com.radusclifos.questlog.user.controller;

import com.radusclifos.questlog.user.dto.SaveFavoritesRequest;
import com.radusclifos.questlog.user.dto.UpdateProfileRequest;
import com.radusclifos.questlog.user.dto.UserResponse;
import com.radusclifos.questlog.user.service.AvatarService;
import com.radusclifos.questlog.user.service.ProfileService;
import com.radusclifos.questlog.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/me")
public class CurrentUserController {

    private final UserService users;
    private final ProfileService profiles;
    private final AvatarService avatars;

    @Autowired
    public CurrentUserController(UserService users, ProfileService profiles, AvatarService avatars) {
        this.users = users;
        this.profiles = profiles;
        this.avatars = avatars;
    }

    @GetMapping
    public UserResponse me(Principal principal) {
        return UserResponse.from(users.current(principal));
    }

    @PatchMapping
    public UserResponse update(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return users.update(principal, request);
    }

    @PutMapping("/favorites")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveFavorites(Principal principal, @Valid @RequestBody SaveFavoritesRequest request) {
        profiles.saveFavorites(principal, request);
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(Principal principal, @RequestParam MultipartFile file) {
        return Map.of("revision", avatars.save(principal, file));
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar(Principal principal) {
        avatars.delete(principal);
    }

}
