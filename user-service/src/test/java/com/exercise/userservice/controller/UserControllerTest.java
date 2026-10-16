package com.exercise.userservice.controller;

import com.exercise.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_returnsCreatedUser() throws Exception {
        JsonNode user = createUser("Suresh Subramaniam");

        assertThat(user.get("id").asInt()).isPositive();
        assertThat(user.get("name").asText()).isEqualTo("Suresh Subramaniam");
        // Microseconds since epoch: 16 digits for any date after 2001.
        assertThat(user.get("createdAt").asLong()).isGreaterThan(1_000_000_000_000_000L);
        assertThat(user.get("updatedAt").asLong()).isEqualTo(user.get("createdAt").asLong());
    }

    @Test
    void createUser_trimsName() throws Exception {
        JsonNode user = createUser("  Lorel Ipsum  ");

        assertThat(user.get("name").asText()).isEqualTo("Lorel Ipsum");
    }

    @Test
    void createUser_rejectsBlankName() throws Exception {
        mockMvc.perform(post("/users").param("name", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("name: must not be blank"));
    }

    @Test
    void createUser_rejectsMissingName() throws Exception {
        mockMvc.perform(post("/users"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("name: must not be blank"));
    }

    @Test
    void getUser_returnsExistingUser() throws Exception {
        int id = createUser("Suresh Subramaniam").get("id").asInt();

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.user.id").value(id))
                .andExpect(jsonPath("$.user.name").value("Suresh Subramaniam"));
    }

    @Test
    void getUser_returns404ForUnknownUser() throws Exception {
        mockMvc.perform(get("/users/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("user 999 not found"));
    }

    @Test
    void getUser_rejectsInvalidId() throws Exception {
        mockMvc.perform(get("/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id: invalid value 'abc'"));

        mockMvc.perform(get("/users/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id: must be greater than or equal to 1"));
    }

    @Test
    void getUsers_returnsNewestFirstAndPaginates() throws Exception {
        createUser("First");
        createUser("Second");
        createUser("Third");

        mockMvc.perform(get("/users").param("pageNum", "1").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.users[*].name", contains("Third", "Second")));

        mockMvc.perform(get("/users").param("pageNum", "2").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].name", contains("First")));
    }

    @Test
    void getUsers_defaultsToTenPerPage() throws Exception {
        for (int i = 0; i < 11; i++) {
            createUser("User " + i);
        }

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(10)));
    }

    @Test
    void getUsers_returnsEmptyListPastLastPage() throws Exception {
        createUser("Only");

        mockMvc.perform(get("/users").param("pageNum", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.users", hasSize(0)));
    }

    @Test
    void getUsers_rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/users").param("pageNum", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("pageNum: must be greater than or equal to 1"));

        mockMvc.perform(get("/users").param("pageSize", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pageSize: invalid value 'abc'"));
    }

    @Test
    void getUsers_filtersByIds() throws Exception {
        int first = createUser("First").get("id").asInt();
        createUser("Second");
        int third = createUser("Third").get("id").asInt();

        // Unknown ids are simply left out.
        mockMvc.perform(get("/users").param("ids", first + "," + third + ",999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.users[*].name", contains("Third", "First")));
    }

    @Test
    void getUsers_rejectsMoreThan100Ids() throws Exception {
        String ids = IntStream.rangeClosed(1, 101)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        mockMvc.perform(get("/users").param("ids", ids))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ids: size must be between 0 and 100"));
    }

    @Test
    void unsupportedRequests_useStandardErrorBody() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.result").value(false));

        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result").value(false));
    }

    private JsonNode createUser(String name) throws Exception {
        String body = mockMvc.perform(post("/users").param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("user");
    }
}