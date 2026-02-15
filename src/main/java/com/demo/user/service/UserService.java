package com.demo.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.user.dto.CreateUserRequest;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.entity.User;
import com.demo.user.repository.UserRepository;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CreateUserResponse registerUser(CreateUserRequest request) {
        User user = userRepository.save(new User(request.email(), request.name()));
        return new CreateUserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
