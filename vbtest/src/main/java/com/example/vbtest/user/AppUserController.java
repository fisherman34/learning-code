package com.example.vbtest.user;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD REST API for users.
 *
 * <pre>
 * GET    /api/app-users       list every user
 * GET    /api/app-users/{id}  read one user
 * POST   /api/app-users       create a user
 * PUT    /api/app-users/{id}  replace username and password
 * DELETE /api/app-users/{id}  remove a user
 * </pre>
 */
@RestController
@RequestMapping(AppUserController.BASE_PATH)
public class AppUserController {

    /** Base path of this resource, also used to build the {@code Location} header. */
    static final String BASE_PATH = "/api/app-users";

    private final AppUserService service;

    public AppUserController(AppUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<AppUserResponse> list() {
        return service.findAll().stream().map(AppUserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AppUserResponse get(@PathVariable Long id) {
        return AppUserResponse.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<AppUserResponse> create(@Valid @RequestBody AppUserRequest request) {
        AppUser created = service.create(request);
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.getId()))
                .body(AppUserResponse.from(created));
    }

    @PutMapping("/{id}")
    public AppUserResponse update(@PathVariable Long id, @Valid @RequestBody AppUserRequest request) {
        return AppUserResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
