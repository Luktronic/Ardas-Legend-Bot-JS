package com.ardaslegends.domain;

import com.ardaslegends.domain.applications.RoleplayApplication;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Embeddable
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "name")
public final class RPChar extends AbstractDomainObject {

    @Column(unique = true)
    private String name;

    @Length(max = 25, message = "Title too long!")
    private String title;

    private String gear;

    private Boolean pvp;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "current_region", foreignKey = @ForeignKey(name = "fk_current_region"))
    @NotNull(message = "RpChar: currentRegion must not be null")
    private Region currentRegion; //the region the character is currently in

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "bound_to", foreignKey = @ForeignKey(name = "fk_bound_to"))
    private Army boundTo; //the army that is bound to this character

    private Boolean injured;
    private Boolean isHealing;
    private LocalDateTime startedHeal;
    private LocalDateTime healEnds;
    private String linkToLore;

    public RPChar(RoleplayApplication application) {
        name = application.getCharacterName();
        title = application.getCharacterTitle();
        gear = application.getGear();
        pvp = application.getPvp();
        currentRegion = application.getFaction().getHomeRegion();

        boundTo = null;
        injured = false;
        isHealing = false;
        startedHeal = null;
        healEnds = null;

        linkToLore = application.getLinkToLore();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RPChar rpChar = (RPChar) o;
        return name.equals(rpChar.name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
