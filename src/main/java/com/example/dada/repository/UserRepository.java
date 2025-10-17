package com.example.dada.repository;

import com.example.dada.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
 * Finds a user by their email address.
 *
 * @param email the email address to search for
 * @return an Optional containing the matching User if found, empty otherwise
 */
Optional<User> findByEmail(String email);
    /**
 * Determines whether a user with the given email address exists.
 *
 * @param email the user's email address to check
 * @return `true` if a user with the specified email exists, `false` otherwise
 */
boolean existsByEmail(String email);
}
