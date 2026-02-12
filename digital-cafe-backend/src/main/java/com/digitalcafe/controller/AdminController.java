package com.digitalcafe.controller;

import com.digitalcafe.dto.request.CreateUserRequest;
import com.digitalcafe.dto.response.UserResponse;
import com.digitalcafe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Controller for managing cafe owners and system operations.
 * Only accessible by users with ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    // =========================================================
    // CREATE CAFE OWNER
    // =========================================================
    @PostMapping("/cafe-owners")
    public ResponseEntity<UserResponse> createCafeOwner(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createCafeOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================
    // GET ALL USERS
    // =========================================================
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    // =========================================================
    // GET USER BY ID
    // =========================================================
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // =========================================================
    // GET USERS BY ROLE
    // =========================================================
    @GetMapping("/users/role/{roleName}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String roleName) {
        List<UserResponse> users = userService.getUsersByRole(roleName);
        return ResponseEntity.ok(users);
    }

    // =========================================================
    // GET PENDING USERS (FOR APPROVAL PANEL)
    // =========================================================
    @GetMapping("/users/pending")
    public ResponseEntity<List<UserResponse>> getPendingUsers() {
        return ResponseEntity.ok(userService.getPendingUsers());
    }

    // =========================================================
    // APPROVE USER
    // =========================================================
    @PostMapping("/users/{id}/approve")
    public ResponseEntity<Map<String,String>> approveUser(@PathVariable Long id){
        userService.approveUser(id);
        return ResponseEntity.ok(Map.of("message","User approved and email sent"));
    }

    // =========================================================
    // REJECT USER
    // =========================================================
    @PostMapping("/users/{id}/reject")
    public ResponseEntity<Map<String,String>> rejectUser(@PathVariable Long id){
        userService.rejectUser(id);
        return ResponseEntity.ok(Map.of("message","User rejected successfully"));
    }

    // =========================================================
    // ACTIVATE USER
    // =========================================================
    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok(Map.of("message", "User activated successfully"));
    }

    // =========================================================
    // DEACTIVATE USER
    // =========================================================
    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
    }

    // =========================================================
    // DELETE USER
    // =========================================================
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}
