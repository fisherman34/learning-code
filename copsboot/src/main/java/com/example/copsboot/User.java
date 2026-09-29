package com.example.copsboot;

import com.sun.istack.NotNull;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "copsboot_user")
public class User {
    @Id
    private UUID id;
    private String email;
    private String password;

    /*
    UserRoleの集合（Set<UserRole>）をデータベースに保存する。
    UserRoleは@Entityではないため、@ElementCollectionを使用して
    Userエンティティとは別のテーブルに保存する。

    fetch = FetchType.EAGER は、Userをデータベースから取得したときに、
    rolesも同時に取得する（即時ロードする）ことを指定している。
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @NotNull
    private Set<UserRole> roles;

    protected User() {
        // Default constructor for JPA
    }

    public User(UUID id, String email, String password, Set<UserRole> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }
}
