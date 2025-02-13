package com.ardaslegends.domain;

import lombok.*;

import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "units")
public final class Unit extends AbstractDomainObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "unit_type", foreignKey = @ForeignKey(name = "fk_unit_unit_type"))
    private UnitType unitType; //The kind of unit, e.g. Gondor Soldier

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "army", foreignKey = @ForeignKey(name = "fk_unit_army"))
    private Army army; // The army in which these units are

    private Integer count; //maximum aamount of those units that are in the army

    private Integer amountAlive; //current alive soldiers
    private Boolean isMounted;

    public Double getCost() {
        return isMounted ? unitType.getTokenCost() + 1 : unitType.getTokenCost();
    }

}
