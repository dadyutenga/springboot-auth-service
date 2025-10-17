package com.example.dada.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, String> profile = new HashMap<>();
        profile.put("email", userDetails.getUsername());
        profile.put("message", "This is a protected endpoint");
        return ResponseEntity.ok(profile);
    }
}
