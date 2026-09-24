package com.tup.reconac.feature.usuario.services.domain;

import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class AuthenticatedUser implements UserDetails, CredentialsContainer {

    private final UUID id;
    private final String email;
    private String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(UUID id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;

        this.authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}