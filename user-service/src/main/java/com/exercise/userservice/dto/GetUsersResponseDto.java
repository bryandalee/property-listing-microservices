package com.exercise.userservice.dto;

import java.util.List;

public class GetUsersResponseDto {

    private boolean result;
    private List<UserDto> users;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public List<UserDto> getUsers() {
        return users;
    }

    public void setUsers(List<UserDto> users) {
        this.users = users;
    }
}