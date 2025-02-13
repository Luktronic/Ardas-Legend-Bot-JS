package com.ardaslegends.domain;

import com.ardaslegends.service.exceptions.logic.faction.FactionServiceException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;

import jakarta.persistence.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Slf4j
@Entity
@Table(name = "factions")
public final class Faction extends AbstractDomainObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name; //unique, name of the faction

    private InitialFaction initialFaction;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Player leader; //the player who leads this faction

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "faction")
    private List<Army> armies; //all current armies of this faction
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "faction")
    private List<Player> players; //all current players of this faction
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "claimedBy")
    private Set<Region> regions; //all regions this faction claims
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "ownedBy")
    private List<ClaimBuild> claimBuilds; //all claimbuilds of this faction

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "faction_allies",
            joinColumns = { @JoinColumn(name = "faction", foreignKey = @ForeignKey(name = "fk_faction_allies_faction"))},
            inverseJoinColumns = { @JoinColumn(name = "ally_faction", foreignKey = @ForeignKey(name = "fk_faction_allies_ally_faction")) })
    private List<Faction> allies; //allies of this faction
    private String colorcode; //the faction's colorcode, used for painting the map

    @Column(name = "role_id", unique = true)
    private Long factionRoleId; // The roleId of the factionRole so that it can be pinged

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Region homeRegion; //Homeregion of the faction

    @Length(max = 512)
    private String factionBuffDescr; //The description of this faction's buff

    @Column(name = "food_stockpile")
    private Integer foodStockpile = 0; // Food stacks in a factions stockpile, these are used for army movements

    @ElementCollection
    @CollectionTable(name = "faction_aliases", joinColumns = @JoinColumn(name = "faction_id", foreignKey = @ForeignKey(name = "fk_faction_aliases_faction_id")))
    private Set<String> aliases = new HashSet<>();

    public Faction(String name, Player leader, List<Army> armies, List<Player> players, Set<Region> regions, List<ClaimBuild> claimBuilds, List<Faction> allies, String colorcode, Region homeRegion, String factionBuffDescr) {
        this.name = name;
        this.leader = leader;
        this.armies = armies;
        this.players = players;
        this.regions = regions;
        this.claimBuilds = claimBuilds;
        this.allies = allies;
        this.colorcode = colorcode;
        this.homeRegion = homeRegion;
        this.factionBuffDescr = factionBuffDescr;
        this.foodStockpile = 0;
    }

    public void addFoodToStockpile(int amount) {
        log.debug("Adding food [amount:{}] to stockpile of faction [{}]", amount, this.name);
        if(amount < 0) {
            log.warn("Amount to add is below 0 [{}]", amount);
            throw FactionServiceException.negativeStockpileAddNotSupported();
        }
        this.foodStockpile += amount;
    }

    public void subtractFoodFromStockpile(int amount) {
        log.debug("Removing food [amount: {}] from stockpile of faction [{}]", amount, this.name);
        if(amount < 0) {
            log.warn("Amount to remove is above 0 [{}]", amount);
            throw FactionServiceException.negativeStockpileSubtractNotSupported();
        }

        if(this.foodStockpile - amount < 0) {
            log.warn("Subtract would set the stockpile of faction [{}] to below zero!", this.name);
            throw FactionServiceException.notEnoughFoodInStockpile(this.name, this.foodStockpile, amount);
        }
        this.foodStockpile -= amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Faction faction = (Faction) o;
        return name.equals(faction.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
