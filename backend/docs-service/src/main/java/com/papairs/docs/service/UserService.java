package com.papairs.docs.service;

import com.papairs.docs.model.User;
import com.papairs.docs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User ensureUserExists(String userId, String username) {
        Optional<User> existingUser = userRepository.findById(userId);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User newUser = new User();
        newUser.setUserId(userId);
        newUser.setUsername(username != null ? username : "User_" + userId.substring(0, 8));
        newUser.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(newUser);
    }

    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }
}
