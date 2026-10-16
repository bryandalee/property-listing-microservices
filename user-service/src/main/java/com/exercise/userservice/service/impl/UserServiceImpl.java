package com.exercise.userservice.service.impl;

import com.exercise.userservice.dto.CreateUserRequestDto;
import com.exercise.userservice.dto.GetUsersRequestDto;
import com.exercise.userservice.dto.GetUsersResponseDto;
import com.exercise.userservice.dto.UserDto;
import com.exercise.userservice.dto.UserResponseDto;
import com.exercise.userservice.entity.User;
import com.exercise.userservice.exception.UserNotFoundException;
import com.exercise.userservice.repository.UserRepository;
import com.exercise.userservice.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    // Newest first; id breaks ties so the order is stable when two users share a timestamp.
    private static final Sort NEWEST_FIRST = Sort.by("createdAt").descending()
            .and(Sort.by("id").descending());

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public GetUsersResponseDto getUsers(GetUsersRequestDto requestDto) {
        PageRequest pageRequest = PageRequest.of(requestDto.getPageNum(),
                requestDto.getPageSize(), NEWEST_FIRST);

        List<User> users = Optional.ofNullable(requestDto.getIds())
                .map(ids -> userRepository.findByIdIn(ids, pageRequest))
                .orElseGet(() -> userRepository.findAll(pageRequest).getContent());

        List<UserDto> userDtoList = users.stream()
                .map(this::convertUserToUserDto)
                .collect(Collectors.toList());

        GetUsersResponseDto responseDto = new GetUsersResponseDto();
        responseDto.setResult(true);
        responseDto.setUsers(userDtoList);

        return responseDto;
    }

    @Override
    public UserResponseDto getUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return toUserResponseDto(user);
    }

    @Override
    public UserResponseDto createUser(CreateUserRequestDto requestDto) {
        User user = new User();
        user.setName(requestDto.getName().trim());

        final Long timestampInMicroSecond = nowInEpochMicroSecond();
        user.setCreatedAt(timestampInMicroSecond);
        user.setUpdatedAt(timestampInMicroSecond);

        userRepository.save(user);

        return toUserResponseDto(user);
    }

    private UserResponseDto toUserResponseDto(User user) {
        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setResult(true);
        responseDto.setUser(convertUserToUserDto(user));

        return responseDto;
    }

    private UserDto convertUserToUserDto(User user) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user, userDto);

        return userDto;
    }

    private Long nowInEpochMicroSecond() {
        return ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
    }
}