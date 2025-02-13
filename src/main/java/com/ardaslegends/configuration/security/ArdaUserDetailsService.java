package com.ardaslegends.configuration.security;

import com.ardaslegends.repository.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Licensed from eu.groeller.datastream, allowed usage by code owner GroellerKarim
 * @author GroellerKarim
 */

@Slf4j

@RequiredArgsConstructor
@Service
public class ArdaUserDetailsService implements UserDetailsService {

    private final PlayerRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        log.debug("Searching for User with id [{}]", id);
        var user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Did not find user with id [{}]!", id);
                    return new UsernameNotFoundException("User with id [" + id + "] not found");
                });
        log.debug("Found user [{}]", user.getIgn());
        return new ArdaUserDetails(user);
    }

}
