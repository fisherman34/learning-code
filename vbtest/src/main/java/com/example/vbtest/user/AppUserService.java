package com.example.vbtest.user;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppUserService {

    private final AppUserRepository repository;

    public AppUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AppUser> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public AppUser findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new AppUserNotFoundException(id));
    }

    public AppUser create(AppUserRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }
        return repository.save(new AppUser(request.username(), request.password()));
    }

    public AppUser update(Long id, AppUserRequest request) {
        AppUser user = findById(id);
        boolean usernameChanged = !user.getUsername().equals(request.username());
        if (usernameChanged && repository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }
        user.setUsername(request.username());
        user.setPassword(request.password());
        return repository.save(user);
    }

    public void delete(Long id) {
        repository.delete(findById(id));
    }
}
