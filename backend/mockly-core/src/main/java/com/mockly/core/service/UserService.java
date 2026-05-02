package com.mockly.core.service;

import com.mockly.core.dto.user.UpdateProfileRequest;
import com.mockly.core.dto.user.UserListResponse;
import com.mockly.core.dto.user.UserResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.core.exception.UserNotFoundException;
import com.mockly.data.entity.Profile;
import com.mockly.data.entity.User;
import com.mockly.data.repository.ProfileRepository;
import com.mockly.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        return toResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + user.getId()));

        return toResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public UserListResponse listInterviewers(String search, int page, int size) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        List<UserResponse> interviewers = profileRepository.findByRole(Profile.ProfileRole.INTERVIEWER)
                .stream()
                .filter(profile -> matchesSearch(profile, normalizedSearch))
                .sorted(Comparator.comparing(profile -> displayNameOrEmail(profile).toLowerCase()))
                .skip((long) Math.max(page, 0) * Math.max(size, 1))
                .limit(Math.max(size, 1))
                .map(profile -> toResponse(profile.getUser(), profile))
                .toList();

        return new UserListResponse(interviewers);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName());
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.level() != null) {
            profile.setLevel(request.level());
        }
        if (request.skills() != null) {
            profile.setSkills(request.skills());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.location() != null) {
            profile.setLocation(request.location());
        }

        profile = profileRepository.save(profile);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        return toResponse(user, profile);
    }

    @Transactional
    public void deleteAccount(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        userRepository.delete(user);
    }

    private UserResponse toResponse(User user, Profile profile) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                displayNameOrEmail(profile),
                profile.getRole(),
                profile.getAvatarUrl(),
                profile.getLevel(),
                profile.getSkills() == null ? List.of() : profile.getSkills(),
                profile.getBio(),
                profile.getLocation(),
                user.getCreatedAt()
        );
    }

    private boolean matchesSearch(Profile profile, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        User user = profile.getUser();
        return contains(profile.getDisplayName(), search)
                || contains(profile.getLevel(), search)
                || contains(profile.getBio(), search)
                || (user != null && contains(user.getEmail(), search));
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private String displayNameOrEmail(Profile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        return profile.getUser() != null ? profile.getUser().getEmail() : "User";
    }
}

