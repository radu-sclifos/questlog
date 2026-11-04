package com.radusclifos.questlog.user.controller;

import com.radusclifos.questlog.user.dto.*;
import com.radusclifos.questlog.user.service.UserService;
import com.radusclifos.questlog.user.service.ProfileService;
import com.radusclifos.questlog.user.service.AvatarService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.security.Principal;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService users;
    private final ProfileService profiles;
    private final AvatarService avatars;

    @Autowired
    public UserController(UserService users, ProfileService profiles, AvatarService avatars) {
        this.users = users;
        this.profiles = profiles;
        this.avatars = avatars;
    }

    @GetMapping("/{username}")
    public UserResponse profile(Principal principal, @PathVariable String username) {
        users.current(principal);
        return UserResponse.from(users.find(username));
    }

    @GetMapping("/{username}/profile")
    public PublicProfileDto publicProfile(@PathVariable String username) {
        return profiles.profile(username);
    }

    @GetMapping("/{username}/avatar")
    public ResponseEntity<byte[]> avatar(@PathVariable String username, WebRequest request) {
        var avatar = avatars.get(username);
        String etag = "\"" + avatar.getRevision() + "\"";
        if (request.checkNotModified(etag)) return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache()).eTag(etag).body(avatar.getData());
    }


}
