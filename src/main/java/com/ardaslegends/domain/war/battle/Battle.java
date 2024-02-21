package com.ardaslegends.domain.war.battle;


import com.ardaslegends.domain.AbstractDomainObject;
import com.ardaslegends.domain.Army;
import com.ardaslegends.domain.Faction;
import com.ardaslegends.domain.war.War;
import com.ardaslegends.domain.war.battle.BattleLocation;
import com.ardaslegends.domain.war.battle.BattleResult;
import com.ardaslegends.service.exceptions.logic.war.BattleServiceException;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;


@Getter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "battles")
public class Battle extends AbstractDomainObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<War> wars;

    private String name;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "battle_attackingArmies",
            joinColumns = { @JoinColumn(name = "battle_id", foreignKey = @ForeignKey(name = "fk_battle_attackingArmies_battle"))},
            inverseJoinColumns = { @JoinColumn(name = "atackingArmy_id", foreignKey = @ForeignKey(name = "fk_battle_attackingArmies_attackingArmy")) })
    private Set<Army> attackingArmies = new HashSet<>(1);

    @NotNull
    @ManyToOne
    @JoinColumn(name = "initial_attacker", foreignKey = @ForeignKey(name = "fk_battle_initial_attacker"))
    private Army initialAttacker;

    @ManyToOne
    @JoinColumn(name = "initial_defender", foreignKey = @ForeignKey(name = "fk_battle_initial_defender"))
    private Faction initialDefender;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "battle_defendingArmies",
            joinColumns = { @JoinColumn(name = "battle_id", foreignKey = @ForeignKey(name = "fk_battle_defendingArmies_battle"))},
            inverseJoinColumns = { @JoinColumn(name = "defendingArmy_id", foreignKey = @ForeignKey(name = "fk_battle_defendingArmies_defendingArmy")) })
    private Set<Army> defendingArmies = new HashSet<>();

    private OffsetDateTime declaredDate;

    private OffsetDateTime timeFrozenFrom;

    private OffsetDateTime timeFrozenUntil;

    private OffsetDateTime agreedBattleDate;

    @Embedded
    private BattleLocation battleLocation;

    @Setter
    @Embedded
    private BattleResult battleResult;

    public Battle(Set<War> wars, String name, Set<Army> attackingArmies, Set<Army> defendingArmies, OffsetDateTime declaredDate, OffsetDateTime timeFrozenFrom, OffsetDateTime timeFrozenUntil, OffsetDateTime agreedBattleDate, BattleLocation battleLocation) {
        this.wars = new HashSet<>(wars);
        this.name = name;
        this.attackingArmies = new HashSet<>(attackingArmies);
        this.defendingArmies = new HashSet<>(defendingArmies);
        this.declaredDate = declaredDate;
        this.timeFrozenFrom = timeFrozenFrom;
        this.timeFrozenUntil = timeFrozenUntil;
        this.agreedBattleDate = agreedBattleDate;
        this.battleLocation = battleLocation;
        this.initialAttacker = attackingArmies.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("CONTACT DEVS: No initial attacker in Battle %s!".formatted(name)));
        if(battleLocation.getFieldBattle())
            this.initialDefender = defendingArmies.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("CONTACT DEVS: No initial defender in Battle %s!".formatted(name))).getFaction();
        else
            this.initialDefender = battleLocation.getClaimBuild().getOwnedBy();
    }
}
