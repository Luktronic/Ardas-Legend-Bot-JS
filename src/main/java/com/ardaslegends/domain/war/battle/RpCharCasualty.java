package com.ardaslegends.domain.war.battle;

import com.ardaslegends.domain.Player;
import com.ardaslegends.domain.RPChar;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter

@Embeddable
public class RpCharCasualty {

    @NotNull
    @ManyToOne
    @JoinColumn(name = "rpchar_id", foreignKey = @ForeignKey(name = "fk_battle_char_casualties_char_id"))
    private RPChar rpChar;

    @ManyToOne
    @JoinColumn(name = "slain_by_id", foreignKey = @ForeignKey(name = "fk_battle_char_casualties_slain_by_id"))
    private Player slainByChar;

    private String slainByWeapon;

    @Setter
    private String optionalCause;

    public RpCharCasualty(RPChar rpChar) {
        Objects.requireNonNull(rpChar, "RpCharCasualty constructor: rpChar was null!");
        this.rpChar = rpChar;
    }

    public RpCharCasualty(RPChar rpChar, Player slainByChar, String slainByWeapon) {
        Objects.requireNonNull(rpChar, "RpCharCasualty constructor: rpChar was null!");
        this.rpChar = rpChar;
        this.slainByChar = slainByChar;
        this.slainByWeapon = slainByWeapon;
    }

    public RpCharCasualty(RPChar rpChar, String optionalCause) {
        Objects.requireNonNull(rpChar, "RpCharCasualty constructor: rpChar was null!");
        this.rpChar = rpChar;
        this.optionalCause = optionalCause;
    }

    @Override
    public String toString() {
        return "RpCharCasualty{" +
                "rpChar=" + rpChar +
                ", slainByChar=" + slainByChar +
                ", slainByWeapon='" + slainByWeapon + '\'' +
                ", optionalCause='" + optionalCause + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RpCharCasualty that = (RpCharCasualty) o;

        if (!rpChar.equals(that.rpChar)) return false;
        if (!Objects.equals(slainByChar, that.slainByChar)) return false;
        if (!Objects.equals(slainByWeapon, that.slainByWeapon))
            return false;
        return Objects.equals(optionalCause, that.optionalCause);
    }

    @Override
    public int hashCode() {
        int result = rpChar.hashCode();
        result = 31 * result + (slainByChar != null ? slainByChar.hashCode() : 0);
        result = 31 * result + (slainByWeapon != null ? slainByWeapon.hashCode() : 0);
        result = 31 * result + (optionalCause != null ? optionalCause.hashCode() : 0);
        return result;
    }
}
