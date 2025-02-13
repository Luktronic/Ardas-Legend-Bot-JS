package com.ardaslegends.domain;

import com.ardaslegends.service.utils.ServiceUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Slf4j
@Entity
@Table(name = "regions")
public final class Region extends AbstractDomainObject {

    @Id
    private String id; //unique, the region id

    private String name; //the name of the region (prob also unique)

    @Enumerated(EnumType.STRING)
    private RegionType regionType; // type of the region

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "faction_claimed_regions",
            joinColumns = { @JoinColumn(name = "region", foreignKey = @ForeignKey(name = "fk_faction_claimed_regions_region"))},
            inverseJoinColumns = { @JoinColumn(name = "faction", foreignKey = @ForeignKey(name = "fk_faction_claimed_regions_faction")) })
    private Set<Faction> claimedBy = new HashSet<>(); //the list of factions which the region is claimed by

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "region")
    private Set<ClaimBuild> claimBuilds = new HashSet<>(); //list of claimbuilds in this region

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "region_neighbours",
            joinColumns = { @JoinColumn(name = "region", foreignKey = @ForeignKey(name = "fk_region_neighbours_region"))},
            inverseJoinColumns = { @JoinColumn(name = "neighbour", foreignKey = @ForeignKey(name = "fk_region_neighbours_neighbour")) })
    private Set<Region> neighboringRegions = new HashSet<>(); //list of neighboring regions

    @Column(name = "has_ownership_changed_since_last_claimmap_update")
    private boolean hasOwnershipChanged;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "currentRegion")
    private Set<RPChar> charsInRegion = new HashSet<>(1);

    public Region(String id, String name, RegionType regionType, Set<Faction> claimedBy, Set<ClaimBuild> claimBuilds, Set<Region> neighboringRegions) {
        this.id = id;
        this.name = name;
        this.regionType = regionType;
        this.claimedBy = claimedBy;
        this.claimBuilds = claimBuilds;
        this.neighboringRegions = neighboringRegions;
        this.hasOwnershipChanged = false;
    }

    /**
     *
     * @param possibleNeighbour, the region that you want to add, Not-Null-Constraint
     * @throws NullPointerException, when parameter is null
     * @return false when region is already in Set, true when it succeeds
     */
    public boolean addNeighbour(@NonNull Region possibleNeighbour) {

        if(neighboringRegions.contains(possibleNeighbour))
            return false;

        neighboringRegions.add(possibleNeighbour);
        return true;

    }

    public void addFactionToClaimedBy(Faction faction) {
        log.debug("Add claiming faction [{}] to region [{}]", faction, this.id);

        Objects.requireNonNull(faction, "Faction must not be nulL!");
        ServiceUtils.checkBlankString(faction.getName(),"faction name");

        if(!this.claimedBy.contains(faction)) {
            log.debug("Faction [{}] is not in region [{}]'s claimedBy Set, adding it", faction, this.id);
            this.claimedBy.add(faction);
            faction.getRegions().add(this);

            this.hasOwnershipChanged = true;
            log.debug("Also setting hasOwnershipChanged to true [ownershipChanged: {}]", this.hasOwnershipChanged);
        }

        log.debug("Faction [{}] is in region [{}]'s claimedBy Set");
    }

    public void removeFactionFromClaimedBy(Faction faction) {
        log.debug("Remove claiming faction [{}] from region [{}]", faction, this.id);

        Objects.requireNonNull(faction, "Faction must not be nulL!");
        ServiceUtils.checkBlankString(faction.getName(),"faction name");

        if(this.getClaimedBy().contains(faction)) {
            log.debug("Faction [{}] is in region [{}]'s claimedBy Set, removing it", faction, this.id);
            this.claimedBy.remove(faction);
            faction.getRegions().remove(this);

            this.hasOwnershipChanged = true;
            log.debug("Also setting hasOwnershipChanged to true [ownershipChanged: {}]", this.hasOwnershipChanged);
        }

        log.debug("Faction [{}] is not in region [{}]'s claimedBy Set", faction, this.id);
    }
    public Set<Region> getNeighboringRegions() {
        return Collections.unmodifiableSet(neighboringRegions);
    }

    public int getCost() {
        return regionType.getCost();
    }

    public boolean hasClaimbuildInRegion(Faction faction) {
        Objects.requireNonNull(faction, "Faction must not be null");

        boolean hasClaimbuild = claimBuilds.stream()
                .anyMatch(claimBuild -> claimBuild.getOwnedBy().equals(faction));

        log.debug("Does faction [{}] have a claimbuild in region [{}]? Returning [{}]", faction.getName(), this.id, hasClaimbuild);
        return hasClaimbuild;
    }

    public boolean isOnlyFactionInRegion(Faction faction) {
        Objects.requireNonNull(faction, "Faction must not be null");

        boolean isOnlyFaction = claimBuilds.stream()
                .anyMatch(claimBuild -> !claimBuild.getOwnedBy().equals(faction));

        log.debug("Is faction [{}] the only faction that has claimbuilds in region [{}]? Returning [{}]", faction.getName(), this.id, isOnlyFaction);
        return isOnlyFaction;
    }

    public boolean hasFactionOtherClaimbuildThan(ClaimBuild cb) {
        Objects.requireNonNull(cb, "claimbuild must not be null");

        boolean hasClaimbuild = claimBuilds.stream()
                .anyMatch(claimBuild -> claimBuild.getOwnedBy().equals(cb.getOwnedBy()) && !claimBuild.equals(cb));

        log.debug("Does faction [{}] have other claimbuild than [{}] in region [{}]? Returning [{}]", cb.getOwnedBy().getName(), cb, this.id, hasClaimbuild);
        return hasClaimbuild;
    }

    public boolean isClaimable(Faction faction) {
        Objects.requireNonNull(faction, "Faction must not be null");

        boolean hasClaimbuild = hasClaimbuildInRegion(faction);
        boolean regionHasNoCb = this.claimBuilds.isEmpty();

        return hasClaimbuild || regionHasNoCb;
    }

    @Override
    public String toString() {
        return "Region{" +
                "id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Region region = (Region) o;
        return id.equals(region.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
