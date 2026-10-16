package com.exercise.userservice.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Integer id) {
        super("user " + id + " not found");
    }
}