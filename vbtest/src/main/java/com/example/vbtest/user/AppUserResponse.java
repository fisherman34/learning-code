package com.example.vbtest.user;

/**
 * Response body for a user.
 *
 * <p>The password is deliberately not part of this representation, so it is never echoed back to
 * callers. Add a field here if a client of this API really needs to read it back.
 */
public record AppUserResponse(Long id, String username) {

    public static AppUserResponse from(AppUser user) {
        return new AppUserResponse(user.getId(), user.getUsername());
    }
}
