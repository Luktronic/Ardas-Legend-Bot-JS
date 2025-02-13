package com.ardaslegends.domain;

import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "armies")
public final class Army extends AbstractDomainObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "army_type")
    @NotNull(message = "Army: Army Type must not be null or empty")
    private ArmyType armyType; //type of the army, either ARMY, TRADING_COMPANY or ARMED_TRADERS

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "faction", foreignKey = @ForeignKey(name = "fk_armies_faction"))
    @NotNull(message = "Army: Faction must not be null")
    private Faction faction; //the faction this army belongs to

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "current_Region", foreignKey = @ForeignKey(name = "fk_armies_current_region"))
    @NotNull(message = "Army: Region must not be null")
    private Region currentRegion; //region the army is currently in

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "boundTo")
    @JoinColumn(name = "bound_to", foreignKey = @ForeignKey(name = "fk_armies_bound_to"))
    private RPChar boundTo; //rp character the army is currently bound to

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, mappedBy = "army")
    private List<Unit> units = new ArrayList<>(); //the units in this army contains

    @ElementCollection
    @CollectionTable(name = "army_sieges",
                joinColumns = @JoinColumn(name = "army_id", foreignKey = @ForeignKey(name = "fk_army_sieges_army_id")))
    private List<String> sieges = new ArrayList<>(); //list of siege equipment this
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "stationed_at", foreignKey = @ForeignKey(name = "fk_armies_stationed_at"))
    private ClaimBuild stationedAt; //claimbuild where this army is stationed

    @NotNull(message = "Army: freeTokens must not be null")
    private Double freeTokens; //how many free unit tokens this army has left

    private Boolean isHealing = false;
    private OffsetDateTime healStart;
    private OffsetDateTime healEnd;
    private Integer hoursHealed;
    private Integer hoursLeftHealing;
    private OffsetDateTime healLastUpdatedAt;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "origin_claimbuild", foreignKey = @ForeignKey(name = "fk_armies_origin_claimbuild"))
    private ClaimBuild originalClaimbuild; //claimbuild where this army was created from

    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "army", cascade = {CascadeType.REMOVE})
    private List<Movement> movements = new ArrayList<>();

    private Boolean isPaid;

    public Army(String name, ArmyType armyType, Faction faction, Region currentRegion, RPChar boundTo, List<Unit> units, List<String> sieges, ClaimBuild stationedAt, Double freeTokens, boolean isHealing, OffsetDateTime healStart, OffsetDateTime healEnd,
                Integer hoursHealed, Integer hoursLeftHealing, ClaimBuild originalClaimbuild, OffsetDateTime createdAt, boolean isPaid) {
        this.name = name;
        this.armyType = armyType;
        this.faction = faction;
        this.currentRegion = currentRegion;
        this.boundTo = boundTo;
        this.units = units;
        this.sieges = sieges;
        this.stationedAt = stationedAt;
        this.freeTokens = freeTokens;
        this.isHealing = isHealing;
        this.healStart = healStart;
        this.healEnd = healEnd;
        this.hoursHealed = hoursHealed;
        this.hoursLeftHealing = hoursLeftHealing;
        this.originalClaimbuild = originalClaimbuild;
        this.createdAt = createdAt;
        this.isPaid = isPaid;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Army army = (Army) o;
        return name.equals(army.name);
    }

    @Override
    public int hashCode() {
        return name != null ? Objects.hash(name):0;
    }

    public boolean allUnitsAlive() {
        return this.units.stream().allMatch(unit -> Objects.equals(unit.getAmountAlive(), unit.getCount()));
    }

    public boolean hasUnitsLeft() { return units.stream().anyMatch(unit -> unit.getAmountAlive() > 0); }

    public Optional<Movement> getActiveMovement() {
        return this.getMovements().stream().filter(Movement::getIsCurrentlyActive).findFirst();
    }

    public int getAmountOfHealHours() {
        double tokensMissing = units.stream()
                .map(unit -> ((unit.getCount()-unit.getAmountAlive())) * unit.getCost())
                .reduce(0.0, Double::sum);
        double hoursHeal = tokensMissing * 24 / 6;
        int divisor = 24;
        if(this.stationedAt.getType().equals(ClaimBuildType.STRONGHOLD)) {
            hoursHeal /= 2;
            divisor = 12;
        }
        int intHoursHeal = (int) Math.ceil(hoursHeal);
        int hoursLeftUntil24h = divisor - (intHoursHeal % divisor);
        return intHoursHeal + hoursLeftUntil24h;
    }

    public void resetHealingStats() {
        this.setIsHealing(false);
        this.setHealStart(null);
        this.setHealEnd(null);
        this.setHoursHealed(0);
        this.setHoursLeftHealing(0);
    }

    public boolean isYoungerThan24h() {
        return OffsetDateTime.now().isBefore(this.createdAt.plusHours(24));
    }
}
