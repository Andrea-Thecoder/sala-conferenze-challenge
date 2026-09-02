package com.naxos.challenge.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Slf4j
public class ExceptionMapper {

    @ServerExceptionMapper
    public RestResponse<ExceptionResponse> mapServiceException(ServiceException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return RestResponse.ResponseBuilder
                .create(Status.BAD_REQUEST,
                        new ExceptionResponse(Status.BAD_REQUEST.getStatusCode(), "BAD_REQUEST", ex.getMessage()))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ExceptionResponse> mapNotFoundException(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return RestResponse.ResponseBuilder
                .create(Status.NOT_FOUND,
                        new ExceptionResponse(Status.NOT_FOUND.getStatusCode(), "NOT_FOUND", ex.getMessage()))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ExceptionResponse> mapNotAuthorizedException(NotAuthorizedException ex) {
        return RestResponse.ResponseBuilder
                .create(Status.UNAUTHORIZED,
                        new ExceptionResponse(Status.UNAUTHORIZED.getStatusCode(), "UNAUTHORIZED", "Authentication required"))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ExceptionResponse> mapForbiddenException(ForbiddenException ex) {
        return RestResponse.ResponseBuilder
                .create(Status.FORBIDDEN,
                        new ExceptionResponse(Status.FORBIDDEN.getStatusCode(), "FORBIDDEN", "Access denied"))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ExceptionResponse> mapInvalidFormatException(InvalidFormatException ex) {
        log.warn("Invalid format: {}", ex.getMessage());
        return RestResponse.ResponseBuilder
                .create(Status.BAD_REQUEST,
                        new ExceptionResponse(Status.BAD_REQUEST.getStatusCode(), "BAD_REQUEST", "Invalid field format: " + ex.getPath()))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ExceptionResponse> mapJsonParseException(JsonParseException ex) {
        log.warn("JSON parse error: {}", ex.getMessage());
        return RestResponse.ResponseBuilder
                .create(Status.BAD_REQUEST,
                        new ExceptionResponse(Status.BAD_REQUEST.getStatusCode(), "BAD_REQUEST", "Malformed JSON request"))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ExceptionResponse> mapRuntimeException(RuntimeException ex) {
        log.error("Unhandled exception", ex);
        return RestResponse.ResponseBuilder
                .create(Status.INTERNAL_SERVER_ERROR,
                        new ExceptionResponse(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "INTERNAL_ERROR", "An unexpected error occurred"))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
