package com.ardaslegends.domain;

import com.ardaslegends.domain.applications.RoleplayApplication;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "rpchars")
public class RPChar extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "owner_id", foreignKey = @ForeignKey(name = "fk_rpchars_owner"))
    private Player owner;

    @Column(unique = true)
    private String name;

    @Length(max = 25, message = "Title too long!")
    private String title;

    private String gear;

    private Boolean pvp;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "current_region", foreignKey = @ForeignKey(name = "fk_rpchar_current_region"))
    @NotNull(message = "RpChar: currentRegion must not be null")
    private Region currentRegion; //the region the character is currently in

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "bound_to", foreignKey = @ForeignKey(name = "fk_rpchar_bound_to"))
    private Army boundTo; //the army that is bound to this character

    @OneToMany(mappedBy = "rpChar")
    private Set<Movement> movements;

    private Boolean injured;
    private Boolean isHealing;
    private OffsetDateTime startedHeal;
    private OffsetDateTime healEnds;
    private OffsetDateTime healLastUpdatedAt;
    private String linkToLore;

    private Boolean active;

    public RPChar(RoleplayApplication application) {
        this.owner = application.getApplicant();
        name = application.getCharacterName();
        title = application.getCharacterTitle();
        gear = application.getGear();
        pvp = application.getPvp();

        currentRegion = application.getFaction().getHomeRegion();
        movements = new HashSet<>();

        boundTo = null;
        injured = false;
        isHealing = false;
        startedHeal = null;
        healEnds = null;
        active = true;

        linkToLore = application.getLinkToLore();
    }

    public RPChar(Player owner, String name, String title, String gear, Boolean pvp, String linkToLore) {
        this.owner = owner;
        this.name = name;
        this.title = title;
        this.gear = gear;
        this.pvp = pvp;

        this.currentRegion = owner.getFaction().getHomeRegion();
        this.movements = new HashSet<>();

        boundTo = null;
        injured = false;
        isHealing = false;
        startedHeal = null;
        healEnds = null;
        active = true;

        this.linkToLore = linkToLore;
    }

    public void injure() {
        setInjured(true);
        Optional.ofNullable(boundTo).ifPresent(army -> {
            army.setBoundTo(null);
            setBoundTo(null);
        });
    }

    public void startHealing() {
        OffsetDateTime now = OffsetDateTime.now();
        setIsHealing(true);
        setStartedHeal(now);
        setHealEnds(now.plusDays(2));
    }
    public Set<Movement> getMovements() {
        return Collections.unmodifiableSet(movements);
    }

    @Override
    public String toString() {
        return name;
    }
}
