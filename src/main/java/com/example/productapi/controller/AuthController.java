package com.example.productapi.controller;

import com.example.productapi.dto.JwtResponse;
import com.example.productapi.dto.LoginRequest;
import com.example.productapi.dto.RegisterRequest;
import com.example.productapi.model.User;
import com.example.productapi.repository.UserRepository;
import com.example.productapi.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final long refreshWindowMs;
    private final long maxSessionMs;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder,
                          AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          @Value("${jwt.refresh-window-ms}") long refreshWindowMs,
                          @Value("${jwt.max-session-ms}") long maxSessionMs) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshWindowMs = refreshWindowMs;
        this.maxSessionMs = maxSessionMs;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepo.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(req.getRole() == null ? "USER" : req.getRole().toUpperCase());
        userRepo.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest req) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        UserDetails user = (UserDetails) auth.getPrincipal();
        String jwt = jwtUtil.generateToken(user.getUsername(), roleOf(user));
        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@AuthenticationPrincipal UserDetails user,
                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String header) {
        Claims claims = jwtUtil.parseClaims(header.substring(7));
        long now = System.currentTimeMillis();
        if (claims.getExpiration().getTime() - now > refreshWindowMs) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le token n'est pas encore proche de l'expiration");
        }
        Long authTime = claims.get("authTime", Long.class);
        if (authTime == null) {
            authTime = claims.getIssuedAt().getTime();
        }
        if (now - authTime > maxSessionMs) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session trop ancienne, reconnectez-vous");
        }
        // Le rôle vient de la base (user), pas de l'ancien token : un changement de rôle est pris en compte
        return ResponseEntity.ok(new JwtResponse(jwtUtil.generateToken(user.getUsername(), roleOf(user), authTime)));
    }

    private static String roleOf(UserDetails user) {
        return user.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}
