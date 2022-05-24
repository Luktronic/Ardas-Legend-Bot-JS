package com.ardaslegends.data.domain;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "factions")
public class Faction {

    @Id
    private String name; //unique, name of the faction

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
    
    private String colorcode; //the faction's colorcode, used for painting the map

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Region homeRegion; //Homeregion of the faction

    @Length(max = 512)
    private String factionBuffDescr; //The description of this faction's buff


}
