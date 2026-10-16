package com.exercise.publicapi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "services.listing.url=http://listing-service",
        "services.user.url=http://user-service"
})
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    // Stands in for the user service: every call it receives must be expected below.
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
    void createUser_forwardsNameToUserService() throws Exception {
        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("name", "Lorel Ipsum");
        services.expect(requestTo("http://user-service/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("""
                        {"result": true, "user": {"id": 1, "name": "Lorel Ipsum",
                         "createdAt": 1475820997000000, "updatedAt": 1475820997000000}}""",
                        MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/public-api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lorel Ipsum\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.name").value("Lorel Ipsum"))
                .andExpect(jsonPath("$.user.createdAt").value(1475820997000000L))
                .andExpect(jsonPath("$.user.updatedAt").value(1475820997000000L));
    }

    @Test
    void createUser_rejectsBlankNameWithoutCallingUserService() throws Exception {
        mockMvc.perform(post("/public-api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("name: must not be blank"));
    }

    @Test
    void createUser_onlyAcceptsJson() throws Exception {
        mockMvc.perform(post("/public-api/users")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Lorel Ipsum"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.result").value(false));
    }

    @Test
    void createUser_returns503WhenUserServiceIsDown() throws Exception {
        services.expect(requestTo("http://user-service/users"))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        mockMvc.perform(post("/public-api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lorel Ipsum\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("user service is unavailable"));
    }
}