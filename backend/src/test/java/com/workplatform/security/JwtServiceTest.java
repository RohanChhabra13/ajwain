package com.workplatform.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-unit-test-secret";

    @Test void roundTripReturnsUserId() {
        JwtService jwt = new JwtService(SECRET, 60);
        assertEquals(42L, jwt.parse(jwt.generate(42L)));
    }

    @Test void tamperedOrForeignTokensAreRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.generate(1L);
        assertNull(jwt.parse(token + "x"));
        assertNull(jwt.parse("not-a-jwt"));
        assertNull(new JwtService("another-secret-another-secret-another-secret-123", 60).parse(token));
    }

    @Test void expiredTokensAreRejected() {
        JwtService jwt = new JwtService(SECRET, -1);
        assertNull(jwt.parse(jwt.generate(1L)));
    }
}
