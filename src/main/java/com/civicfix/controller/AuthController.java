package com.civicfix.controller;

import com.civicfix.dto.AuthResponse;
import com.civicfix.dto.LoginRequest;
import com.civicfix.dto.RegisterRequest;
import com.civicfix.dto.UserDto;
import com.civicfix.entity.User;
import com.civicfix.security.JwtTokenProvider;
import com.civicfix.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);
        String token = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, UserDto.fromEntity(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        User user = userService.getUserByEmail(request.getEmail());
        String token = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, UserDto.fromEntity(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }
}
