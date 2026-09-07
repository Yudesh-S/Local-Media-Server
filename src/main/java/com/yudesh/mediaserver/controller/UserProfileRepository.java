package com.yudesh.mediaserver.controller;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<UserProfile> findByNameIgnoreCase(String name);
}