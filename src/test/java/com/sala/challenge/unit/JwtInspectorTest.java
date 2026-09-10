package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.security.JwtInspector;

import jakarta.ws.rs.ForbiddenException;

@ExtendWith(MockitoExtension.class)
class JwtInspectorTest {

    @Mock
    JsonWebToken jwt;

    @InjectMocks
    JwtInspector jwtInspector;

    @Test
    void getSubject_validUuidSubjectClaim_returnsUuid() {
        UUID subject = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn(subject.toString());

        UUID result = jwtInspector.getSubject();

        assertThat(result).isEqualTo(subject);
    }

    @Test
    void sameSubject_subjectMatchesJwtSubject_returnsTrue() {
        UUID subject = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn(subject.toString());

        boolean result = jwtInspector.sameSubject(subject);

        assertThat(result).isTrue();
    }

    @Test
    void sameSubject_subjectDiffersFromJwtSubject_returnsFalse() {
        when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());

        boolean result = jwtInspector.sameSubject(UUID.randomUUID());

        assertThat(result).isFalse();
    }

    @Test
    void sameSubject_nullSubject_returnsFalse() {
        boolean result = jwtInspector.sameSubject(null);

        assertThat(result).isFalse();
    }

    @Test
    void getRole_groupsContainValidRole_returnsRole() {
        when(jwt.getGroups()).thenReturn(Set.of("ADMIN"));

        Role result = jwtInspector.getRole();

        assertThat(result).isEqualTo(Role.ADMIN);
    }

    @Test
    void getRole_groupsEmpty_returnsNull() {
        when(jwt.getGroups()).thenReturn(Set.of());

        Role result = jwtInspector.getRole();

        assertThat(result).isNull();
    }

    @Test
    void hasRole_roleMatchesJwtRole_returnsTrue() {
        when(jwt.getGroups()).thenReturn(Set.of("ORGANIZER"));

        boolean result = jwtInspector.hasRole(Role.ORGANIZER);

        assertThat(result).isTrue();
    }

    @Test
    void hasRole_roleDiffersFromJwtRole_returnsFalse() {
        when(jwt.getGroups()).thenReturn(Set.of("ORGANIZER"));

        boolean result = jwtInspector.hasRole(Role.ADMIN);

        assertThat(result).isFalse();
    }

    @Test
    void hasRole_roleArgumentIsNull_returnsFalse() {
        boolean result = jwtInspector.hasRole(null);

        assertThat(result).isFalse();
    }

    @Test
    void hasRole_jwtHasNoRole_returnsFalse() {
        when(jwt.getGroups()).thenReturn(Set.of());

        boolean result = jwtInspector.hasRole(Role.CUSTOMER);

        assertThat(result).isFalse();
    }

    @Test
    void checkAccessAllowed_customerAccessingOwnResource_doesNotThrow() {
        UUID subject = UUID.randomUUID();
        when(jwt.getGroups()).thenReturn(Set.of("CUSTOMER"));
        when(jwt.getSubject()).thenReturn(subject.toString());

        assertThatCode(() -> jwtInspector.checkAccessAllowed(subject)).doesNotThrowAnyException();
    }

    @Test
    void checkAccessAllowed_customerAccessingOtherResource_throwsForbiddenException() {
        when(jwt.getGroups()).thenReturn(Set.of("CUSTOMER"));
        when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());

        Throwable thrown = catchThrowable(() -> jwtInspector.checkAccessAllowed(UUID.randomUUID()));

        assertThat(thrown).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void checkAccessAllowed_adminAccessingOtherResource_doesNotThrow() {
        when(jwt.getGroups()).thenReturn(Set.of("ADMIN"));

        assertThatCode(() -> jwtInspector.checkAccessAllowed(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void checkAccessAllowed_organizerAccessingOtherResource_doesNotThrow() {
        when(jwt.getGroups()).thenReturn(Set.of("ORGANIZER"));

        assertThatCode(() -> jwtInspector.checkAccessAllowed(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void checkAccessAllowed_unmappableRoleAccessingOwnResource_doesNotThrow() {
        UUID subject = UUID.randomUUID();
        when(jwt.getGroups()).thenReturn(Set.of());
        when(jwt.getSubject()).thenReturn(subject.toString());

        assertThatCode(() -> jwtInspector.checkAccessAllowed(subject)).doesNotThrowAnyException();
    }

    @Test
    void checkAccessAllowed_unmappableRoleAccessingOtherResource_throwsForbiddenException() {
        when(jwt.getGroups()).thenReturn(Set.of());
        when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());

        Throwable thrown = catchThrowable(() -> jwtInspector.checkAccessAllowed(UUID.randomUUID()));

        assertThat(thrown).isInstanceOf(ForbiddenException.class);
    }
}
