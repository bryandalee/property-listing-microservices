package com.exercise.publicapi.client;

import com.exercise.publicapi.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Talks to the user service over its REST API. The public API never reads the users database directly.
 */
@Component
public class UserServiceClient {

    private static final String SERVICE_NAME = "user service";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserServiceClient(RestTemplate restTemplate, @Value("${services.user.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Optional<UserDto> getUser(int id) {
        return ServiceCall.execute(SERVICE_NAME, () -> {
            try {
                UserResponse response = restTemplate.getForObject(baseUrl + "/users/{id}", UserResponse.class, id);
                return Optional.of(response.user());
            } catch (HttpClientErrorException.NotFound e) {
                return Optional.empty();
            }
        });
    }

    /**
     * Looks up several users in a single call to the user service. Ids that don't exist are left out.
     */
    public Map<Integer, UserDto> getUsersByIds(Collection<Integer> ids) {
        Set<Integer> distinctIds = new LinkedHashSet<>(ids);
        if (distinctIds.isEmpty()) {
            return Map.of();
        }

        URI url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/users")
                .queryParam("ids", distinctIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                // GET /users is paginated, so ask for a page big enough to hold every requested user.
                .queryParam("pageSize", distinctIds.size())
                .build().toUri();

        List<UserDto> users = ServiceCall.execute(SERVICE_NAME,
                () -> restTemplate.getForObject(url, UsersResponse.class).users());

        return users.stream().collect(Collectors.toMap(UserDto::id, Function.identity()));
    }

    public UserDto createUser(String name) {
        // The user service expects application/x-www-form-urlencoded, not JSON.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", name);

        return ServiceCall.execute(SERVICE_NAME,
                () -> restTemplate.postForObject(baseUrl + "/users", form, UserResponse.class).user());
    }

    // Response bodies of the user service. Fields we don't need (like "result") are ignored.
    private record UserResponse(UserDto user) {
    }

    private record UsersResponse(List<UserDto> users) {
    }
}