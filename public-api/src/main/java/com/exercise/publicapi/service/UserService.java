package com.exercise.publicapi.service;

import com.exercise.publicapi.dto.CreateUserRequestDto;
import com.exercise.publicapi.dto.CreateUserResponseDto;

public interface UserService {

    CreateUserResponseDto createUser(CreateUserRequestDto requestDto);
}