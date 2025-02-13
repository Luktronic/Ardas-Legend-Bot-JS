package com.ardaslegends.domain.war;

import com.ardaslegends.domain.AbstractDomainObject;
import com.ardaslegends.domain.Faction;
import com.ardaslegends.domain.war.battle.Battle;
import com.ardaslegends.service.exceptions.logic.war.WarServiceException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j

@Entity
@Table(name = "wars")
public class War extends AbstractDomainObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @ElementCollection
    @CollectionTable(name = "war_aggressors",
            joinColumns = @JoinColumn(name = "war_id", foreignKey = @ForeignKey(name = "fk_war_aggressors_war_id")))
    private Set<WarParticipant> aggressors = new HashSet<>(2);

    @ElementCollection
    @CollectionTable(name = "war_defenders",
            joinColumns = @JoinColumn(name = "war_id", foreignKey = @ForeignKey(name = "fk_war_defenders_war_id")))
    private Set<WarParticipant> defenders = new HashSet<>(2);

    @NotNull
    private OffsetDateTime startDate;

    @Setter(AccessLevel.PRIVATE)
    private OffsetDateTime endDate;

    @Setter(AccessLevel.PRIVATE)
    @NotNull
    private Boolean isActive;

    @ManyToMany(mappedBy = "wars", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Battle> battles = new HashSet<>(4);

    // TODO: Test worthy
    public War(String name, Faction aggressor, Faction defender) {
        Objects.requireNonNull(name, "WarConstructor: Name must not be null");
        Objects.requireNonNull(aggressor, "WarConstructor: Aggressor must not be null");
        Objects.requireNonNull(defender, "WarConstructor: Defender must not be null");

        var warDeclarationDate = OffsetDateTime.now();

        log.trace("Creating Aggressor WarParticipantObject");
        WarParticipant aggressorWarParticipant = new WarParticipant(aggressor, true, warDeclarationDate);

        log.trace("Creating Defender WarParticipantObject");
        WarParticipant defenderWarParticipant = new WarParticipant(defender, true, warDeclarationDate);

        this.name = name;
        this.aggressors.add(aggressorWarParticipant);
        this.defenders.add(defenderWarParticipant);

        this.startDate = warDeclarationDate;
        this.isActive = true;
    }

    @NotNull
    public Set<WarParticipant> getEnemies(Faction faction) {
        // If the faction is in the aggressors -> return defenders
        var containsAggressor = this.aggressors.stream()
                .map(participant -> participant.getWarParticipant())
                .anyMatch(aggressor -> aggressor.equals(faction));

        if (containsAggressor)
            return this.defenders;

        var containsDefenders = this.defenders.stream()
                .map(participant -> participant.getWarParticipant())
                .anyMatch(defender -> defender.equals(faction));

        // If the faction is in defenders -> return aggressors
        if(containsDefenders)
            return this.aggressors;

        return new HashSet<WarParticipant>();
    }

    public WarParticipant getInitialAttacker() {
        return getInitialParty(this.aggressors);
    }

    public WarParticipant getInitialDefender() {
        return getInitialParty(this.defenders);
    }

    private WarParticipant getInitialParty(Set<WarParticipant> participants) {
        return participants.stream()
                .filter(warParticipant -> warParticipant.getInitialParty())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Found no initialParty in War '%s'".formatted(this.name)));
    }

    private <T> void addToSet(Set<T> set, T object, WarServiceException exception) {
        var successfullyAdded = set.add(object);

        if(!successfullyAdded) {
            log.warn("Could not add {} [{}] because it is already present in Set", object.getClass().getSimpleName(), object.toString());
            throw exception;
        }
    }

    private WarParticipant buildWarParticipant(Faction faction) {
        Objects.requireNonNull(faction, "Faction must not be null!");
        return new WarParticipant(faction, false, OffsetDateTime.now());
    }

    public void addToAggressors(Faction faction) {
        val participant = buildWarParticipant(faction);
        addToSet(this.aggressors, participant, WarServiceException.factionAlreadyJoinedTheWarAsAttacker(participant.getWarParticipant().getName()));
    }

    public void addToDefenders(Faction faction) {
        val participant = buildWarParticipant(faction);
        addToSet(this.defenders, participant, WarServiceException.factionAlreadyJoinedTheWarAsDefender(participant.getWarParticipant().getName()));
    }

    public void addToBattles(Battle battle) {
        addToSet(this.battles, battle, WarServiceException.battleAlreadyListed(battle.getName()));
    }

    public void end() {
        log.debug("Setting war [{}] to inactive", name);
        setIsActive(false);

        val endDate = OffsetDateTime.now();
        log.debug("Setting war [{}] end date to [{}]",name, endDate);
        setEndDate(endDate);
    }

    public Set<WarParticipant> getAggressors() {
        return Collections.unmodifiableSet(this.aggressors);
    }

    public Set<WarParticipant> getDefenders() {
        return Collections.unmodifiableSet(this.defenders);
    }

    public Set<Battle> getBattles() {
        return Collections.unmodifiableSet(this.battles);
    }

    @Override
    public String toString() {
        return "War{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", aggressors=" + aggressors +
                ", defenders=" + defenders +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", battles=" + battles +
                '}';
    }
}
