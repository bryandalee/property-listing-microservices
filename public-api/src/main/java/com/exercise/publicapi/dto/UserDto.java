package com.exercise.publicapi.dto;

public record UserDto(
        Integer id,
        String name,
        Long createdAt,
        Long updatedAt
) {
}