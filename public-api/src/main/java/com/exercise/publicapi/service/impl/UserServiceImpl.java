package com.exercise.publicapi.service.impl;

import com.exercise.publicapi.client.UserServiceClient;
import com.exercise.publicapi.dto.CreateUserRequestDto;
import com.exercise.publicapi.dto.CreateUserResponseDto;
import com.exercise.publicapi.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserServiceClient userServiceClient;

    public UserServiceImpl(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    @Override
    public CreateUserResponseDto createUser(CreateUserRequestDto requestDto) {
        return new CreateUserResponseDto(userServiceClient.createUser(requestDto.name()));
    }
}