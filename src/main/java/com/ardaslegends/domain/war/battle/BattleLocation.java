package com.ardaslegends.domain.war.battle;

import com.ardaslegends.domain.ClaimBuild;
import com.ardaslegends.domain.Region;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

import java.util.Objects;

@Getter

@Embeddable
public class BattleLocation {

    @ManyToOne
    @JoinColumn(name = "region_id", foreignKey = @ForeignKey(name = "fk_battle_location_region_id"))
    private Region region;

    private Boolean fieldBattle;

    @ManyToOne
    @JoinColumn(name = "claimbuild_id", foreignKey = @ForeignKey(name = "fk_battle_location_claimbuild_id"))
    private ClaimBuild claimBuild;

    public BattleLocation(Region region,boolean fieldBattle,ClaimBuild claimBuild){
        this.region = region;
        this.fieldBattle = fieldBattle;
        this.claimBuild = claimBuild;
    }

    public BattleLocation() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BattleLocation that = (BattleLocation) o;

        if (!region.equals(that.region)) return false;
        if (!fieldBattle.equals(that.fieldBattle)) return false;
        return Objects.equals(claimBuild, that.claimBuild);
    }

    @Override
    public int hashCode() {
        int result = region.hashCode();
        result = 31 * result + fieldBattle.hashCode();
        result = 31 * result + (claimBuild != null ? claimBuild.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BattleLocation{" +
                "region=" + region +
                ", fieldBattle=" + fieldBattle +
                ", claimBuild=" + claimBuild +
                '}';
    }
}
