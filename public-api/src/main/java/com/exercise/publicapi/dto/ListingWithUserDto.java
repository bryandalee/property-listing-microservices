package com.exercise.publicapi.dto;

/**
 * A listing as returned by GET /public-api/listings: the owner is embedded instead of a bare userId.
 */
public record ListingWithUserDto(
        Integer id,
        String listingType,
        Integer price,
        Long createdAt,
        Long updatedAt,
        UserDto user
) {
}