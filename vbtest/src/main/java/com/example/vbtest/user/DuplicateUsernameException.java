package com.example.vbtest.user;

/** Thrown when a create or update would collide with an existing username. */
public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("Username already in use: " + username);
    }
}
