package com.ardaslegends.domain.war.battle;

import com.ardaslegends.domain.Army;
import com.ardaslegends.domain.Unit;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor
@Getter

@Embeddable
public class UnitCasualty {

    @NotNull
    @ManyToOne
    @JoinColumn(name = "unit_id", foreignKey = @ForeignKey(name = "fk_battle_unit_casualties_unit_id"))
    private Unit unit;

    @NotNull
    private Long amount;

    public UnitCasualty(Unit unit, Long amount) {
        Objects.requireNonNull(unit, "UnitCasualty constructor: unit was null!");
        Objects.requireNonNull(amount, "UnitCasualty constructor: amount was null!");

        this.unit = unit;
        this.amount = amount;
    }

    public Army getArmy() {
        return unit.getArmy();
    }

    @Override
    public String toString() {
        return "UnitCasualty{" +
                "unit=" + unit +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnitCasualty that = (UnitCasualty) o;

        if (!unit.equals(that.unit)) return false;
        return amount.equals(that.amount);
    }

    @Override
    public int hashCode() {
        int result = unit.hashCode();
        result = 31 * result + amount.hashCode();
        return result;
    }
}
