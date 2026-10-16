package com.exercise.publicapi.dto;

public record ListingDto(
        Integer id,
        Integer userId,
        String listingType,
        Integer price,
        Long createdAt,
        Long updatedAt
) {
}