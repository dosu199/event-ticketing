package com.ticketing.booking.api;

import java.util.regex.Pattern;

/**
 * TEMPORARY identity until Keycloak login (Phase 2).
 *
 * <p>The frontend sends a random guest id in the X-User-Id header. It is NOT secure: anyone
 * can send any id. Phase 2 replaces it with the user id from a verified JWT, and this class
 * goes away. The id ends up in Redis key names, so it's restricted to safe characters.
 */
final class UserIds {

    static final String HEADER = "X-User-Id";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private UserIds() {
    }

    static String require(String raw) {
        if (raw == null || !VALID.matcher(raw).matches()) {
            throw new InvalidUserIdException();
        }
        return raw;
    }

    static String optional(String raw) {
        return raw == null || raw.isBlank() ? null : require(raw);
    }

    static class InvalidUserIdException extends RuntimeException {
        InvalidUserIdException() {
            super("Header " + HEADER + " must be 1-64 characters: letters, digits or '-'");
        }
    }
}
