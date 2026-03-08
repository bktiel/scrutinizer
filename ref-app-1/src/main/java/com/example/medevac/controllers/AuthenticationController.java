package com.example.medevac.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.HttpClientErrorException;

@Controller
public class AuthenticationController {
    @GetMapping("/api/v1/validateSession")
    public ResponseEntity<String> isValidSession(@Nullable Authentication authentication) {
        return new ResponseEntity<>(authentication == null ? HttpStatus.UNAUTHORIZED : HttpStatus.OK);
    }
}
