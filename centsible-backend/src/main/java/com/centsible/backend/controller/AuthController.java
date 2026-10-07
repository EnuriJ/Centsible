package com.centsible.backend.controller;

import com.centsible.backend.dto.AuthResponseDTO;
import com.centsible.backend.dto.LoginRequestDTO;
import com.centsible.backend.dto.SignUpRequestDTO;
import com.centsible.backend.dto.UserDTO;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.UserRepository;
import com.centsible.backend.security.CustomUserDetails;
import com.centsible.backend.security.JwtService;
import com.centsible.backend.service.CategorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CategorizationService categorizationService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          CategorizationService categorizationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.categorizationService = categorizationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignUpRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email is already registered. Please sign in instead."));
        }

        String displayName = request.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = email.substring(0, email.indexOf('@'));
        }

        User newUser = new User(email, passwordEncoder.encode(request.getPassword()), displayName);
        User savedUser = userRepository.save(newUser);

        // Seed default categories & keyword rules specifically for this new user
        categorizationService.seedDefaultCategoriesAndRules(savedUser);

        String token = jwtService.generateToken(savedUser);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getEmail(), savedUser.getDisplayName());

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponseDTO(token, userDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password."));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        String token = jwtService.generateToken(user);
        UserDTO userDTO = new UserDTO(user.getId(), user.getEmail(), user.getDisplayName());

        return ResponseEntity.ok(new AuthResponseDTO(token, userDTO));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        User user = userDetails.getUser();
        return ResponseEntity.ok(new UserDTO(user.getId(), user.getEmail(), user.getDisplayName()));
    }
}
