package com.example.vbtest.user;

/** Thrown when no user exists for the requested id. */
public class AppUserNotFoundException extends RuntimeException {

    public AppUserNotFoundException(Long id) {
        super("No user with id " + id);
    }
}
