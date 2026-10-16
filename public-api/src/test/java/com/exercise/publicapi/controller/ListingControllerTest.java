package com.exercise.publicapi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "services.listing.url=http://listing-service",
        "services.user.url=http://user-service"
})
@AutoConfigureMockMvc
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    // Stands in for the listing and user services: every call they receive must be expected below.
    private MockRestServiceServer services;

    @BeforeEach
    void setUp() {
        services = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @AfterEach
    void verifyAllExpectedCallsWereMade() {
        services.verify();
    }

    @Test
    void getListings_embedsTheOwnerOfEachListing() throws Exception {
        services.expect(requestTo("http://listing-service/listings?pageNum=1&pageSize=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"result": true, "listings": [
                          %s, %s, %s
                        ]}""".formatted(listing(3, 1), listing(2, 2), listing(1, 1)), MediaType.APPLICATION_JSON));
        // All owners come from one call, and user 1 is asked for once even though they own two listings.
        services.expect(requestTo("http://user-service/users?ids=1,2&pageSize=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"result\": true, \"users\": [%s, %s]}".formatted(
                        user(2, "Lorel Ipsum"), user(1, "Suresh Subramaniam")), MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/public-api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.listings.length()").value(3))
                .andExpect(jsonPath("$.listings[0].id").value(3))
                .andExpect(jsonPath("$.listings[0].listingType").value("rent"))
                .andExpect(jsonPath("$.listings[0].price").value(6000))
                .andExpect(jsonPath("$.listings[0].userId").doesNotExist())
                .andExpect(jsonPath("$.listings[0].user.name").value("Suresh Subramaniam"))
                .andExpect(jsonPath("$.listings[1].user.name").value("Lorel Ipsum"))
                .andExpect(jsonPath("$.listings[2].user.name").value("Suresh Subramaniam"));
    }

    @Test
    void getListings_forwardsPaginationAndUserFilter() throws Exception {
        services.expect(requestTo("http://listing-service/listings?pageNum=2&pageSize=5&userId=7"))
                .andRespond(withSuccess("{\"result\": true, \"listings\": []}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/public-api/listings")
                        .param("pageNum", "2")
                        .param("pageSize", "5")
                        .param("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.listings.length()").value(0));
    }

    @Test
    void getListings_returnsNullUserWhenOwnerDoesNotExist() throws Exception {
        services.expect(requestTo("http://listing-service/listings?pageNum=1&pageSize=10"))
                .andRespond(withSuccess("{\"result\": true, \"listings\": [%s]}".formatted(listing(1, 42)),
                        MediaType.APPLICATION_JSON));

        services.expect(requestTo("http://user-service/users?ids=42&pageSize=1"))
                .andRespond(withSuccess("{\"result\": true, \"users\": []}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/public-api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings[0].id").value(1))
                .andExpect(jsonPath("$.listings[0].user").isEmpty());
    }

    @Test
    void getListings_rejectsInvalidPaginationWithoutCallingServices() throws Exception {
        mockMvc.perform(get("/public-api/listings").param("pageNum", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("pageNum: must be greater than or equal to 1"));

        mockMvc.perform(get("/public-api/listings").param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pageSize: must be less than or equal to 100"));
    }

    @Test
    void getListings_returns503WhenListingServiceIsDown() throws Exception {
        services.expect(requestTo("http://listing-service/listings?pageNum=1&pageSize=10"))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        mockMvc.perform(get("/public-api/listings"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("listing service is unavailable"));
    }

    @Test
    void getListings_returns502WhenUserServiceFails() throws Exception {
        services.expect(requestTo("http://listing-service/listings?pageNum=1&pageSize=10"))
                .andRespond(withSuccess("{\"result\": true, \"listings\": [%s]}".formatted(listing(1, 1)),
                        MediaType.APPLICATION_JSON));
        services.expect(requestTo("http://user-service/users?ids=1&pageSize=1"))
                .andRespond(withServerError());

        mockMvc.perform(get("/public-api/listings"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("user service failed to handle the request"));
    }

    @Test
    void createListing_createsListingForExistingUser() throws Exception {
        services.expect(requestTo("http://user-service/users/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userResponse(1, "Suresh Subramaniam"), MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("userId", "1");
        expectedForm.add("listingType", "rent");
        expectedForm.add("price", "6000");
        services.expect(requestTo("http://listing-service/listings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("{\"result\": true, \"listing\": %s}".formatted(listing(143, 1)),
                        MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/public-api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1, \"listingType\": \"rent\", \"price\": 6000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listing.id").value(143))
                .andExpect(jsonPath("$.listing.userId").value(1))
                .andExpect(jsonPath("$.listing.listingType").value("rent"))
                .andExpect(jsonPath("$.listing.price").value(6000))
                .andExpect(jsonPath("$.listing.createdAt").value(1475820997000000L));
    }

    @Test
    void createListing_rejectsUnknownUserWithoutCreatingListing() throws Exception {
        services.expect(requestTo("http://user-service/users/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"result\": false, \"message\": \"user 99 not found\"}"));

        mockMvc.perform(post("/public-api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 99, \"listingType\": \"rent\", \"price\": 6000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("user 99 does not exist"));
    }

    @Test
    void createListing_validatesBodyWithoutCallingServices() throws Exception {
        mockMvc.perform(post("/public-api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("listingType: must not be blank; price: must not be null; userId: must not be null"));

        mockMvc.perform(post("/public-api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1, \"listingType\": \"house\", \"price\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("listingType: must match \"rent|sale\"; price: must be greater than or equal to 1"));
    }

    @Test
    void createListing_rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/public-api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1, \"listingType\": \"rent\", \"price\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("request body is missing or is not valid JSON"));
    }

    @Test
    void createListing_passesThroughListingServiceRejection() throws Exception {
        services.expect(requestTo("http://user-service/users/1"))
                .andRespond(withSuccess(userResponse(1, "Suresh Subramaniam"), MediaType.APPLICATION_JSON));
        services.expect(requestTo("http://listing-service/listings"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"result\": false, \"message\": \"listing rejected\"}"));

        mockMvc.perform(post("/public-api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1, \"listingType\": \"rent\", \"price\": 6000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("listing rejected"));
    }

    private static String listing(int id, int userId) {
        return """
                {"id": %d, "userId": %d, "listingType": "rent", "price": 6000,
                 "createdAt": 1475820997000000, "updatedAt": 1475820997000000}""".formatted(id, userId);
    }

    private static String user(int id, String name) {
        return """
                {"id": %d, "name": "%s", "createdAt": 1475820997000000, "updatedAt": 1475820997000000}"""
                .formatted(id, name);
    }

    private static String userResponse(int id, String name) {
        return "{\"result\": true, \"user\": %s}".formatted(user(id, name));
    }
}