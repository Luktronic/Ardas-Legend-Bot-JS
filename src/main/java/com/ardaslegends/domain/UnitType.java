package com.ardaslegends.domain;

import lombok.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "unit_types")
public final class UnitType extends AbstractDomainObject {

    @Id
    private String unitName; //unique, the name of this Unit
    @NotNull(message = "UnitType: tokenCost must not be null")
    private Double tokenCost; //how much tokens this unit costs

}
