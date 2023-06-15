package com.ardaslegends.repository.player;

import com.ardaslegends.domain.Player;
import com.ardaslegends.domain.QPlayer;
import com.ardaslegends.domain.QRPChar;
import com.ardaslegends.repository.exceptions.PlayerRepositoryException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PlayerRepositoryImpl extends QuerydslRepositorySupport implements PlayerRepositoryCustom {
    public PlayerRepositoryImpl() {
        super(Player.class);
    }

    /**
     * Fetches a player object that corresponds with the given discordId
     * @param discordId which the queried player should have
     * @return a non-null player object
     * @throws NullPointerException if any parameter is null
     * @throws PlayerRepositoryException if no player was found
     */
    @Override
    public @NonNull Player queryByDiscordId(String discordId) {
        Objects.requireNonNull(discordId, "DiscordId must not be null!");

        val qplayer = QPlayer.player;

        val fetchedPlayer = from(qplayer)
                .where(qplayer.discordID.eq(discordId))
                .fetchFirst();

        if(fetchedPlayer == null) { throw PlayerRepositoryException.entityNotFound("discordId", discordId); }
        return fetchedPlayer;
    }

    /**
     * @param discordIds, query parameter, null values will be filtered out
     * @return a set of players, size does not have to match discordIds size
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public @NonNull Set<Player> queryByDiscordId(@NonNull String[] discordIds) {
        Objects.requireNonNull(discordIds, "DiscordIds must not be null");
        val qplayer = QPlayer.player;

        val filteredNullsSet = Arrays.stream(discordIds)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        val fetchedPlayers = from(qplayer)
                .where(qplayer.discordID.in(filteredNullsSet))
                .fetch();

        return new HashSet<>(fetchedPlayers);

    }

    @Override
    public Optional<Player> queryPlayerByRpChar(String name) {
        Objects.requireNonNull(name, "Name must not be null");

        val qPlayer = QPlayer.player;
        val qRpChar = QRPChar.rPChar;

        val joinedRpChars = new QRPChar("rpCharacters");

        val result = from(qPlayer)
                .innerJoin(qPlayer.rpChars, joinedRpChars)
                .where(joinedRpChars.name.equalsIgnoreCase(name))
                .fetchFirst();


        return Optional.ofNullable(result);
    }

    @Override
    public List<Player> queryPlayersWithHealingRpchars() {

        val qPlayer = QPlayer.player;

        val joinedRpChars = new QRPChar("rpCharacters");

        return from(qPlayer)
                .innerJoin(qPlayer.rpChars, joinedRpChars)
                .where(joinedRpChars.isHealing.isTrue())
                .fetch();
    }
}
