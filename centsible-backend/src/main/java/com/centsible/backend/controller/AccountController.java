package com.centsible.backend.controller;

import com.centsible.backend.config.DataSeeder;
import com.centsible.backend.dto.DeleteAccountRequestDTO;
import com.centsible.backend.model.User;
import com.centsible.backend.security.CustomUserDetails;
import com.centsible.backend.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    public AccountController(PasswordEncoder passwordEncoder, AccountService accountService) {
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
    }

    /**
     * Permanently deletes the signed-in user's account and all of their data. Requires the
     * account password, so a stolen or left-open session can't wipe an account on its own.
     *
     * Wrong-password responses are 403, not 401: the frontend treats any 401 as "session
     * expired" and signs the user out, which would be the wrong reaction to a typo.
     */
    @DeleteMapping
    public ResponseEntity<?> deleteAccount(@Valid @RequestBody DeleteAccountRequestDTO request,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();

        if (DataSeeder.DEMO_EMAIL.equalsIgnoreCase(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "The shared demo account can't be deleted."));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Incorrect password."));
        }

        accountService.deleteAccountAndAllData(user.getId());
        return ResponseEntity.noContent().build();
    }
}
