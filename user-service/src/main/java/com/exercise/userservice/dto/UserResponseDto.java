package com.exercise.userservice.dto;

/**
 * Response body for both "get specific user" and "create user", which share the same shape.
 */
public class UserResponseDto {

    private boolean result;
    private UserDto user;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}