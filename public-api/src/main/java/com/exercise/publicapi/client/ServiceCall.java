package com.exercise.publicapi.client;

import com.exercise.publicapi.exception.DownstreamServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

/**
 * Runs a call to another service and turns its failures into a {@link DownstreamServiceException}
 */
final class ServiceCall {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ServiceCall() {
    }

    static <T> T execute(String serviceName, Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            // The service rejected the request (e.g. a validation error)-> pass the answer through.
            throw new DownstreamServiceException(e.getStatusCode(), errorMessage(e), e);
        } catch (HttpServerErrorException e) {
            throw new DownstreamServiceException(HttpStatus.BAD_GATEWAY,
                    serviceName + " failed to handle the request", e);
        } catch (ResourceAccessException e) {
            // Connection refused or timed out
            throw new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    serviceName + " is unavailable", e);
        }
    }

    // Services answer errors with {"result": false, "message": "..."}
    private static String errorMessage(HttpClientErrorException e) {
        try {
            JsonNode message = OBJECT_MAPPER.readTree(e.getResponseBodyAsString()).get("message");
            if (message != null && message.isTextual()) {
                return message.asText();
            }
        } catch (Exception ignored) {
            // Not the usual error body; fall back to the HTTP status text.
        }
        return e.getStatusText();
    }
}