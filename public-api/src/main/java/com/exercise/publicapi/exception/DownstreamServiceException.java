package com.exercise.publicapi.exception;

import org.springframework.http.HttpStatusCode;

/**
 * A call to the listing or user service did not succeed. Carry the status the public API should answer with
 */
public class DownstreamServiceException extends RuntimeException {

    private final HttpStatusCode status;

    public DownstreamServiceException(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}