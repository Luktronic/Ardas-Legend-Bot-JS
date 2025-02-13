package com.ardaslegends.domain;

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
@Table(name = "claimbuilds")
public final class ClaimBuild extends AbstractEntity {

    @Column(unique = true)
    private String name; //unique, name of the claimbuild

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "region", foreignKey = @ForeignKey(name = "fk_claimbuild_region"))
    @NotNull(message = "Claimbuild: Region must not be null")
    private Region region; //the region the claimbuild is in

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Claimbuild: ClaimbuildType must not be null")
    private ClaimBuildType type; //Type of claimbuild, e.g. HAMLET

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "owned_by", foreignKey = @ForeignKey(name = "fk_claimbuild_owned_by"))
    @NotNull(message = "Claimbuild: ownedBy must not be null")
    private Faction ownedBy; //faction which owns this CB

    @Embedded
    @NotNull(message = "Claimbuild: Coordinate must not be null")
    private Coordinate coordinates; //coordinate locations

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "stationedAt")
    private List<Army> stationedArmies = new ArrayList<>(); //armies which are stationed in this CB

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "originalClaimbuild")
    private List<Army> createdArmies = new ArrayList<>(); //armies which were created from this CB. Usually only 1 army, but capitals can create 2

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, mappedBy = "claimbuild")
    private List<ProductionClaimbuild> productionSites = new ArrayList<>(); //the production sites in this cb

    @ElementCollection(targetClass = SpecialBuilding.class)
    @CollectionTable(name = "claimbuild_special_buildings",
            joinColumns = @JoinColumn(name = "claimbuild_id", foreignKey = @ForeignKey(name = "fk_claimbuild_special_buildings_claimbuild_id")))
    @Column(name = "special_buildings")
    @Enumerated(EnumType.STRING)
    private List<SpecialBuilding> specialBuildings = new ArrayList<>(); //special buildings in this cb, e.g. House of Healing

    private String traders; //traders in this CB. e.g. Dwarven Smith. Only relevant for staff so they know which traders need to be added

    private String siege; //the siege equipment the cb provides
    private String numberOfHouses; //houses in this CB, e.g. 4 large 12 small. Only relevant for staff

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "claimbuild_builders",
            joinColumns = { @JoinColumn(name = "claimbuild_id", foreignKey = @ForeignKey(name = "fk_claimbuild_builders_claimbuild_id"))},
            inverseJoinColumns = { @JoinColumn(name = "player_id", foreignKey = @ForeignKey(name = "fk_claimbuild_builders_player_id")) })
    private Set<Player> builtBy = new HashSet<>(); //the player who built the CB

    private int freeArmiesRemaining; // Every new army decrements this attribute until its at 0
    private int freeTradingCompaniesRemaining; // Every new trading decrements this attribute until its at 0

    public ClaimBuild(String name, Region region, ClaimBuildType type, Faction ownedBy, Coordinate coordinates, List<SpecialBuilding> specialBuildings, String traders, String siege, String numberOfHouses, Set<Player> builtBy) {
        this.name = name;
        this.region = region;
        this.type = type;
        this.ownedBy = ownedBy;
        this.coordinates = coordinates;
        this.productionSites = new ArrayList<>();
        this.specialBuildings = new ArrayList<>(specialBuildings);
        this.traders = traders;
        this.siege = siege;
        this.numberOfHouses = numberOfHouses;
        this.builtBy = new HashSet<>(builtBy);

        this.createdArmies = new ArrayList<>();
        this.stationedArmies = new ArrayList<>();

        this.freeArmiesRemaining = type.getFreeArmies();
        this.freeTradingCompaniesRemaining = type.getFreeTradingCompanies();
    }
    public ClaimBuild(String name, Region region, ClaimBuildType type, Faction ownedBy, Coordinate coordinates, List<ProductionClaimbuild> productionSites, List<SpecialBuilding> specialBuildings, String traders, String siege, String numberOfHouses, Set<Player> builtBy) {
        this(name, region, type, ownedBy, coordinates, specialBuildings, traders, siege, numberOfHouses, builtBy);
        this.productionSites = productionSites;
    }


    public int getCountOfArmies() {
        int count = (int) createdArmies.stream()
                .filter(army -> ArmyType.ARMY.equals(army.getArmyType()))
                .count();
        log.debug("Claimbuild [{}] has created [{}] armies", this.name, count);
        return count;
    }

    public int getCountOfTradingCompanies() {
        int count = (int) createdArmies.stream()
                .filter(army -> ArmyType.TRADING_COMPANY.equals(army.getArmyType()))
                .count();
        log.debug("Claimbuild [{}] has created [{}] trading companies", this.name, count);
        return count;
    }
    public boolean atMaxArmies() {
        int countOfArmies = getCountOfArmies();
        int maxArmies = getType().getMaxArmies();

        if(countOfArmies >= maxArmies) {
            log.debug("Claimbuild [{}] is at max armies, max armies [{}] - armies created [{}]", this.name, maxArmies, countOfArmies);
            return true;
        }
        log.debug("Claimbuild [{}] can create more armies. max armies [{}] - armies created [{}]", this.name, maxArmies, countOfArmies);
        return false;
    }

    public boolean atMaxTradingCompanies() {
        int countOfTradingCompanies = getCountOfTradingCompanies();
        int maxTradingCompanies = getType().getMaxTradingCompanies();

        if(countOfTradingCompanies >= maxTradingCompanies) {
            log.debug("Claimbuild [{}] is at max trading companies, max companies[{}] - companies created [{}]", this.name, maxTradingCompanies, countOfTradingCompanies);
            return true;
        }
        log.debug("Claimbuild [{}] can create more trading companies. max companies [{}] - companies created [{}]", this.name, maxTradingCompanies, countOfTradingCompanies);
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}
