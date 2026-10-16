package com.exercise.publicapi.dto;

public record ErrorDto(
        boolean result,
        String message
) {
}