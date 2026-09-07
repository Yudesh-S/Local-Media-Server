package com.yudesh.mediaserver.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserProfileRepository userRepository;
    private final WatchProgressRepository progressRepository;

    public UserProfileService(
            UserProfileRepository userRepository,
            WatchProgressRepository progressRepository
    ) {
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
    }

    public List<UserProfile> getUsers() {
        return userRepository.findAll(
                Sort.by(Sort.Direction.ASC, "name")
        );
    }

    public Optional<UserProfile> findUser(Long id) {
        return userRepository.findById(id);
    }

    public UserProfile createUser(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "User name cannot be blank."
            );
        }

        String cleanedName = name.trim();

        if (cleanedName.length() > 50) {
            throw new IllegalArgumentException(
                    "User name cannot be longer than 50 characters."
            );
        }

        if (userRepository.existsByNameIgnoreCase(cleanedName)) {
            throw new IllegalArgumentException(
                    "A user with that name already exists."
            );
        }

        return userRepository.save(
                new UserProfile(cleanedName)
        );
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }

        progressRepository.deleteAllByUser_Id(id);
        userRepository.deleteById(id);

        return true;
    }
}