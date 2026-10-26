package com.radusclifos.questlog.auth.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record Token(String headerName, String parameterName, String token) { }

    @GetMapping("/csrf")
    public Token csrf(CsrfToken token) {
        return new Token(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

}
