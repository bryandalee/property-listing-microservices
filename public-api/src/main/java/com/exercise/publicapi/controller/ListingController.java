package com.exercise.publicapi.controller;

import com.exercise.publicapi.dto.CreateListingRequestDto;
import com.exercise.publicapi.dto.CreateListingResponseDto;
import com.exercise.publicapi.dto.GetListingsResponseDto;
import com.exercise.publicapi.service.ListingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/public-api")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/listings")
    public GetListingsResponseDto getListings(
            @RequestParam(name = "pageNum", defaultValue = "1") @Min(1) Integer pageNum,
            // Capped so the owners of a page always fit in one user service lookup (max 100 ids).
            @RequestParam(name = "pageSize", defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
            @RequestParam(name = "userId", required = false) @Min(1) Integer userId
    ) {
        return listingService.getListings(pageNum, pageSize, userId);
    }

    @PostMapping("/listings")
    public CreateListingResponseDto createListing(@Valid @RequestBody CreateListingRequestDto requestDto) {
        return listingService.createListing(requestDto);
    }
}