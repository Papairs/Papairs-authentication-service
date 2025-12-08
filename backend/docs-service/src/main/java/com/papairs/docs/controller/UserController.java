package com.papairs.docs.controller;

import com.papairs.docs.dto.request.CreateUserRequest;
import com.papairs.docs.model.User;
import com.papairs.docs.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docs")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        User response = userService.createUser(createUserRequest.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
