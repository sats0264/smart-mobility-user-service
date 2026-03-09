package com.mobilitypass.user_mobility.controller;

import com.mobilitypass.user_mobility.beans.UserProfile;
import com.mobilitypass.user_mobility.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserProfile>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{keycloakId}/suspend")
    public ResponseEntity<UserProfile> suspendUser(
            @PathVariable String keycloakId,
            @RequestParam(required = false, defaultValue = "Suspension administrative") String reason) {
        return ResponseEntity.ok(userService.suspendUser(keycloakId, reason));
    }

    @PostMapping("/{keycloakId}/reactivate")
    public ResponseEntity<UserProfile> reactivateUser(@PathVariable String keycloakId) {
        return ResponseEntity.ok(userService.reactivateUser(keycloakId));
    }
}
