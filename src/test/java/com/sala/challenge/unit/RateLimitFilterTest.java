package com.sala.challenge.unit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.security.LoginRateLimiter;
import com.sala.challenge.security.RateLimitFilter;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    HttpServerRequest vertxRequest;

    @Mock
    SocketAddress socketAddress;

    @Mock
    LoginRateLimiter rateLimiter;

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    UriInfo uriInfo;

    @InjectMocks
    RateLimitFilter filter;

    @Test
    void filter_withinLimit_doesNotAbortRequest() throws Exception {
        when(vertxRequest.remoteAddress()).thenReturn(socketAddress);
        when(socketAddress.host()).thenReturn("1.2.3.4");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("auth/login");
        when(rateLimiter.isAllowed("auth/login:1.2.3.4")).thenReturn(true);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void filter_beyondLimit_abortsWithTooManyRequests() throws Exception {
        when(vertxRequest.remoteAddress()).thenReturn(socketAddress);
        when(socketAddress.host()).thenReturn("1.2.3.4");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("auth/login");
        when(rateLimiter.isAllowed("auth/login:1.2.3.4")).thenReturn(false);

        filter.filter(requestContext);

        verify(requestContext).abortWith(org.mockito.ArgumentMatchers.argThat(response ->
                ((Response) response).getStatus() == Response.Status.TOO_MANY_REQUESTS.getStatusCode()));
    }

    @Test
    void filter_ratingLimiterThrows_failsOpenWithoutAborting() throws Exception {
        when(vertxRequest.remoteAddress()).thenReturn(socketAddress);
        when(socketAddress.host()).thenReturn("1.2.3.4");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("auth/login");
        when(rateLimiter.isAllowed("auth/login:1.2.3.4")).thenThrow(new RuntimeException("Redis down"));

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void filter_ipExtractionFails_failsOpenWithoutAborting() throws Exception {
        when(vertxRequest.remoteAddress()).thenThrow(new RuntimeException("no remote address"));

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }
}
