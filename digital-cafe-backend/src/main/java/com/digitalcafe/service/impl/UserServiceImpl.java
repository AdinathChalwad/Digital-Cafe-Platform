package com.digitalcafe.service.impl;

import com.digitalcafe.dto.request.CreateStaffRequest;
import com.digitalcafe.dto.request.CreateUserRequest;
import com.digitalcafe.dto.response.UserResponse;
import com.digitalcafe.entity.*;
import com.digitalcafe.exception.BadRequestException;
import com.digitalcafe.exception.ResourceNotFoundException;
import com.digitalcafe.repository.CafeRepository;
import com.digitalcafe.repository.RoleRepository;
import com.digitalcafe.repository.UserRepository;
import com.digitalcafe.service.EmailService;
import com.digitalcafe.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CafeRepository cafeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;


// ======================================================
// REGISTER CAFE OWNER
// ======================================================

    @Override
    @Transactional
    public UserResponse createCafeOwner(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail()))
            throw new BadRequestException("Email already registered");

        Role ownerRole = roleRepository.findByName(Role.RoleName.CAFE_OWNER)
                .orElseThrow(() -> new ResourceNotFoundException("Role","name","CAFE_OWNER"));

        User user = User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .password(null)
                .roles(Collections.singleton(ownerRole))
                .isActive(true)
                .isEmailVerified(false)
                .isProfileComplete(false)
                .mustResetPassword(false)
                .profileCompletionPercentage(0)
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        return mapToUserResponse(userRepository.save(user));
    }

// ======================================================
// CREATE STAFF
// ======================================================

    @Override
    public UserResponse createChef(Long cafeId, CreateUserRequest request) {
        return createStaffUser(cafeId, request, Role.RoleName.CHEF);
    }

    @Override
    public UserResponse createWaiter(Long cafeId, CreateUserRequest request) {
        return createStaffUser(cafeId, request, Role.RoleName.WAITER);
    }

    @Override
    public UserResponse createStaff(CreateStaffRequest request, String roleName) {

        Role.RoleName roleEnum = Role.RoleName.valueOf(roleName);

        CreateUserRequest userReq = new CreateUserRequest();
        userReq.setEmail(request.getEmail());

        return createStaffUser(request.getCafeId(), userReq, roleEnum);
    }

    private UserResponse createStaffUser(Long cafeId, CreateUserRequest request, Role.RoleName roleName){

        if(userRepository.existsByEmail(request.getEmail()))
            throw new BadRequestException("Email already registered");

        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cafe","id",cafeId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role","name",roleName));

        User creator = getCurrentUserEntity();

        User user = User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .password(null)
                .roles(Collections.singleton(role))
                .cafe(cafe)
                .createdByUser(creator)
                .isActive(true)
                .isEmailVerified(false)
                .isProfileComplete(false)
                .mustResetPassword(false)
                .profileCompletionPercentage(0)
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        return mapToUserResponse(userRepository.save(user));
    }

// ======================================================
// ADMIN APPROVAL FLOW
// ======================================================


    @Override
    @Transactional
    public void approveUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User","id",id));

        if(user.getStatus() != UserStatus.VERIFIED)
            throw new BadRequestException("User must verify email before approval");

        user.setStatus(UserStatus.APPROVED);
        user.setApprovedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("User approved by admin: {}", user.getEmail());
    }


//    @Override
//    @Transactional
//    public void rejectUser(Long id) {
//
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("User","id",id));
//
//        user.setStatus(UserStatus.REJECTED);
//        userRepository.save(user);
//    }


    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING_VERIFICATION)
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }


// ======================================================
// SET PASSWORD VIA EMAIL LINK
// ======================================================




// ======================================================
// USER MANAGEMENT
// ======================================================

    @Override
    public UserResponse getUserById(Long id){
        return mapToUserResponse(getUserEntity(id));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id,CreateUserRequest request){

        User user = getUserEntity(id);

        if(!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail()))
            throw new BadRequestException("Email already exists");

        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());

        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    public void activateUser(Long id){
        User user=getUserEntity(id);
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long id){
        User user=getUserEntity(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public UserResponse toggleUserStatus(Long id,boolean isActive){

        User user=getUserEntity(id);
        user.setIsActive(isActive);

        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id){
        userRepository.delete(getUserEntity(id));
    }

// ======================================================
// FETCH USERS
// ======================================================

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable){
        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    @Override
    public List<UserResponse> getUsersByRole(String roleName){

        Role.RoleName roleEnum = Role.RoleName.valueOf(roleName);

        return userRepository.findByRoleName(roleEnum)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<UserResponse> getUsersByCafe(Long cafeId, Pageable pageable){
        return userRepository.findByCafeId(cafeId,pageable)
                .map(this::mapToUserResponse);
    }

    @Override
    public List<UserResponse> getStaffByCafeId(Long cafeId){
        return userRepository.findByCafeId(cafeId)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getChefsByCafeId(Long cafeId){
        return userRepository.findByCafeIdAndRoleName(cafeId,Role.RoleName.CHEF)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getWaitersByCafeId(Long cafeId){
        return userRepository.findByCafeIdAndRoleName(cafeId,Role.RoleName.WAITER)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

// ======================================================
// CURRENT USER
// ======================================================

    @Override
    public UserResponse getCurrentUser(){
        return mapToUserResponse(getCurrentUserEntity());
    }

    private User getCurrentUserEntity(){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User","email",email));
    }

    private User getUserEntity(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User","id",id));
    }

// ======================================================
// MAPPER
// ======================================================

    private UserResponse mapToUserResponse(User user){

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .isProfileComplete(user.getIsProfileComplete())
                .profileCompletionPercentage(user.getProfileCompletionPercentage())
                .mustResetPassword(user.getMustResetPassword())
                .status(user.getStatus().name())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .cafeName(user.getCafe()!=null?user.getCafe().getName():null)
                .cafeId(user.getCafe()!=null?user.getCafe().getId():null)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
