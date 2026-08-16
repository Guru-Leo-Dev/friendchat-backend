package com.friendchat.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CurrentUser implements UserDetails {
	private static final long serialVersionUID = 1L;
	private final UUID id;
    private final String email;

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public String  getPassword()                    { return null; }
    @Override public String  getUsername()                    { return email; }
    @Override public boolean isAccountNonExpired()            { return true; }
    @Override public boolean isAccountNonLocked()             { return true; }
    @Override public boolean isCredentialsNonExpired()        { return true; }
    @Override public boolean isEnabled()                      { return true; }
}
