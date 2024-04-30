package com.ardaslegends.service.auth;

import com.ardaslegends.domain.Player;
import org.springframework.security.core.userdetails.UserDetails;

public interface TokenService {
    Player extractPlayer(String token);
    String generateAuthenticationToken(Player player);
    String generateRegistrationtoken(String discordId);
    boolean isTokenValid(String token, UserDetails userDetails);
}
