package com.exercise.publicapi.dto;

import java.util.List;

public record GetListingsResponseDto(
        boolean result,
        List<ListingWithUserDto> listings
) {
}