package com.exercise.userservice.controller;

import com.exercise.userservice.dto.CreateUserRequestDto;
import com.exercise.userservice.dto.GetUsersRequestDto;
import com.exercise.userservice.dto.GetUsersResponseDto;
import com.exercise.userservice.dto.UserResponseDto;
import com.exercise.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public GetUsersResponseDto getUsers(
            @RequestParam(name = "pageNum", defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(name = "ids", required = false) @Size(max = 100) List<Integer> ids
    ) {
        GetUsersRequestDto requestDto = new GetUsersRequestDto();
        requestDto.setPageNum(pageNum - 1);
        requestDto.setPageSize(pageSize);
        requestDto.setIds(ids);

        return userService.getUsers(requestDto);
    }

    @GetMapping("/users/{id}")
    public UserResponseDto getUser(@PathVariable("id") @Min(1) Integer id) {
        return userService.getUser(id);
    }

    @PostMapping("/users")
    public UserResponseDto createUser(@Valid CreateUserRequestDto requestDto) {
        return userService.createUser(requestDto);
    }
}