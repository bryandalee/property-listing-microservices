package com.exercise.publicapi.service.impl;

import com.exercise.publicapi.client.ListingServiceClient;
import com.exercise.publicapi.client.UserServiceClient;
import com.exercise.publicapi.dto.CreateListingRequestDto;
import com.exercise.publicapi.dto.CreateListingResponseDto;
import com.exercise.publicapi.dto.GetListingsResponseDto;
import com.exercise.publicapi.dto.ListingDto;
import com.exercise.publicapi.dto.ListingWithUserDto;
import com.exercise.publicapi.dto.UserDto;
import com.exercise.publicapi.exception.InvalidRequestException;
import com.exercise.publicapi.service.ListingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ListingServiceImpl implements ListingService {

    private final ListingServiceClient listingServiceClient;
    private final UserServiceClient userServiceClient;

    public ListingServiceImpl(ListingServiceClient listingServiceClient, UserServiceClient userServiceClient) {
        this.listingServiceClient = listingServiceClient;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public GetListingsResponseDto getListings(int pageNum, int pageSize, Integer userId) {
        List<ListingDto> listings = listingServiceClient.getListings(pageNum, pageSize, userId);

        // Fetch every owner on this page up front instead of once per listing.
        List<Integer> userIds = listings.stream().map(ListingDto::userId).toList();
        Map<Integer, UserDto> usersById = userServiceClient.getUsersByIds(userIds);

        List<ListingWithUserDto> listingsWithUser = listings.stream()
                .map(listing -> new ListingWithUserDto(
                        listing.id(),
                        listing.listingType(),
                        listing.price(),
                        listing.createdAt(),
                        listing.updatedAt(),
                        // null if the owner no longer exists in the user service
                        usersById.get(listing.userId())))
                .toList();

        return new GetListingsResponseDto(true, listingsWithUser);
    }

    @Override
    public CreateListingResponseDto createListing(CreateListingRequestDto requestDto) {
        // The listing service doesn't know about users, so the public API checks the owner exists.
        if (userServiceClient.getUser(requestDto.userId()).isEmpty()) {
            throw new InvalidRequestException("user " + requestDto.userId() + " does not exist");
        }

        ListingDto listing = listingServiceClient.createListing(
                requestDto.userId(), requestDto.listingType(), requestDto.price());

        return new CreateListingResponseDto(listing);
    }
}