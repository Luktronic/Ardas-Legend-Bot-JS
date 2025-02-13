package com.ardaslegends.presentation.api.response.war;

import com.ardaslegends.domain.Army;

public record BattleArmyResponse(
        String nameOfArmy,
        String nameOfFaction,
        String boundPlayerIgn
) {
    public BattleArmyResponse(Army army) {
        this(
                army.getName(),
                army.getFaction().getName(),
                army.getBoundTo() == null ? null : army.getBoundTo().getOwner().getIgn()
        );
    }
}
