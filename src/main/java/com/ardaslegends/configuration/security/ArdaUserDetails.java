package com.ardaslegends.configuration.security;

import com.ardaslegends.domain.Player;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Licensed from eu.groeller.datastream, allowed usage by code owner GroellerKarim
 * @author GroellerKarim
 */

public class ArdaUserDetails implements UserDetails {

    @Getter
    private final Player player;

    public ArdaUserDetails(@NonNull Player player) {
        this.player = player;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        // TODO: Figure out what the password is -> probably check if the discord token is correct or smth
        // TODO: Coding at 1 am doesn't help my brain here.
        return null;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
