package com.digitalcafe.service;

import com.digitalcafe.dto.request.CreateStaffRequest;
import com.digitalcafe.dto.request.CreateUserRequest;
import com.digitalcafe.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    // ================= REGISTRATION =================

    UserResponse createCafeOwner(CreateUserRequest request);

    UserResponse createChef(Long cafeId, CreateUserRequest request);

    UserResponse createWaiter(Long cafeId, CreateUserRequest request);

    UserResponse createStaff(CreateStaffRequest request, String roleName);

    // ================= ADMIN APPROVAL =================

    void approveUser(Long id);

//    void rejectUser(Long id);

    List<UserResponse> getPendingUsers();

    // ================= PASSWORD SETUP =================

//    void setPassword(String token, String password);

    // ================= USER MANAGEMENT =================

    UserResponse getUserById(Long id);

    UserResponse updateUser(Long id, CreateUserRequest request);

    void activateUser(Long id);

    void deactivateUser(Long id);

    UserResponse toggleUserStatus(Long id, boolean isActive);

    void deleteUser(Long id);

    // ================= FETCH USERS =================

    Page<UserResponse> getAllUsers(Pageable pageable);

    List<UserResponse> getUsersByRole(String roleName);

    Page<UserResponse> getUsersByCafe(Long cafeId, Pageable pageable);

    List<UserResponse> getStaffByCafeId(Long cafeId);

    List<UserResponse> getChefsByCafeId(Long cafeId);

    List<UserResponse> getWaitersByCafeId(Long cafeId);

    // ================= CURRENT USER =================

    UserResponse getCurrentUser();



}
