package com.ardaslegends.domain;

import com.ardaslegends.service.exceptions.logic.player.PlayerServiceException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Slf4j
@Entity
@Table(name = "players")
public final class Player extends AbstractDomainObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @NotNull(message = "Player: IGN must not be null")
    private String ign; //unique, ingame name of the player

    @Column(unique = true)
    @NotNull(message = "Player: UUID must not be null")
    private String uuid;

    @Column(name = "discord_id", unique = true)
    @NotNull(message = "Player: DiscordID must not be null")
    private String discordID; //unique, the ID of the player's discord account

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "faction", foreignKey = @ForeignKey(name = "fk_player_faction"))
    @NotNull(message = "Player: Faction must not be null")
    private Faction faction; //the faction this character belongs to


    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private Set<RPChar> rpChars = new HashSet<>(); //the player's rp character

    @ManyToMany(mappedBy = "builtBy", cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private List<ClaimBuild> builtClaimbuilds;

    private Boolean isStaff;

    public Player(String ign, String uuid, String discordID, Faction faction, RPChar rpChar) {
        Objects.requireNonNull(rpChar);

        this.ign = ign;
        this.uuid = uuid;
        this.discordID = discordID;
        this.faction = faction;
        this.rpChars = new HashSet<>(Set.of(rpChar));
        this.builtClaimbuilds = new ArrayList<>(1);
        this.isStaff = false;
    }


    public Player(String ign, String uuid, String discordID, Faction faction) {

        this.ign = ign;
        this.uuid = uuid;
        this.discordID = discordID;
        this.faction = faction;
        this.rpChars = new HashSet<>(1);
        this.builtClaimbuilds = new ArrayList<>(1);
        this.isStaff = false;
    }

    public Optional<RPChar> getActiveCharacter() {
        return rpChars.stream()
                .filter(RPChar::getActive)
                .findFirst();
    }
    public Set<RPChar> getRpChars() {
        return Collections.unmodifiableSet(rpChars);
    }

    public void addActiveRpChar(RPChar rpChar) {
        Objects.requireNonNull(rpChar);
        if(this.rpChars == null) this.rpChars = new HashSet<>();

        this.rpChars.stream()
                .filter(RPChar::getActive)
                .forEach(this::clearRelations);

        if (!this.rpChars.add(rpChar)) {
            throw PlayerServiceException.rpcharAlreadyExists(rpChar.getName());
        }
        rpChar.setActive(true);
    }

    public RPChar deleteCharacter() {
        val character = getActiveCharacter().orElseThrow(PlayerServiceException::noRpChar);
        clearRelations(character);
        return character;
    }

    private void clearRelations(RPChar rpchar) {
        rpchar.setActive(false);

        Optional.ofNullable(rpchar.getBoundTo()).ifPresent(army -> {
            army.setBoundTo(null);
            rpchar.setBoundTo(null);

            army.getMovements().stream()
                    .filter(Movement::getIsCurrentlyActive)
                    .findFirst().ifPresent(movement -> {
                        // TODO: Decide on a way to handle active movements

                    });
        });

        // TODO: Handle active battles
    }

    public List<ClaimBuild> getBuiltClaimbuilds() {
        return Collections.unmodifiableList(builtClaimbuilds);
    }

    @Override
    public String toString() {
        return ign;
    }
}
