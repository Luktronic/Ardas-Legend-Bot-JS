package com.ardaslegends.presentation.api.response.war;

import com.ardaslegends.domain.war.battle.Battle;
import com.ardaslegends.domain.war.War;

public record BattleResponse(
        String[] wars,
        String nameOfBattle,

        BattleArmyResponse[] attackingArmies,

        BattleArmyResponse[] defendingArmies

) {
    public BattleResponse(Battle battle) {
        this(
               battle.getWars().stream().map(War::getName).toArray(String[]::new),
               battle.getName(),

               battle.getAttackingArmies().stream()
                       .map(BattleArmyResponse::new)
                       .toArray(BattleArmyResponse[]::new),

                battle.getDefendingArmies().stream()
                        .map(BattleArmyResponse::new)
                        .toArray(BattleArmyResponse[]::new)
        );
    }
}
