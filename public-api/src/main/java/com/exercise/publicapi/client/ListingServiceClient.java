package com.exercise.publicapi.client;

import com.exercise.publicapi.dto.ListingDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Talks to the listing service over its REST API. The public API never reads the listings database directly.
 */
@Component
public class ListingServiceClient {

    private static final String SERVICE_NAME = "listing service";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ListingServiceClient(RestTemplate restTemplate, @Value("${services.listing.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<ListingDto> getListings(int pageNum, int pageSize, Integer userId) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/listings")
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize);
        if (userId != null) {
            uri.queryParam("userId", userId);
        }
        URI url = uri.build().toUri();

        return ServiceCall.execute(SERVICE_NAME,
                () -> restTemplate.getForObject(url, ListingsResponse.class).listings());
    }

    public ListingDto createListing(int userId, String listingType, int price) {
        // The listing service expects application/x-www-form-urlencoded, not JSON
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("userId", String.valueOf(userId));
        form.add("listingType", listingType);
        form.add("price", String.valueOf(price));

        return ServiceCall.execute(SERVICE_NAME,
                () -> restTemplate.postForObject(baseUrl + "/listings", form, ListingResponse.class).listing());
    }

    // Response bodies of the listing service. Fields we don't need (like "result") are ignored
    private record ListingsResponse(List<ListingDto> listings) {
    }

    private record ListingResponse(ListingDto listing) {
    }
}