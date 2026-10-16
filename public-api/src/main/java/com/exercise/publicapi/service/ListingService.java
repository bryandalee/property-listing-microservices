package com.exercise.publicapi.service;

import com.exercise.publicapi.dto.CreateListingRequestDto;
import com.exercise.publicapi.dto.CreateListingResponseDto;
import com.exercise.publicapi.dto.GetListingsResponseDto;

public interface ListingService {

    GetListingsResponseDto getListings(int pageNum, int pageSize, Integer userId);

    CreateListingResponseDto createListing(CreateListingRequestDto requestDto);
}