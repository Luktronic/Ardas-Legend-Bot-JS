package com.ardaslegends.service.dto.war.battle;

public record ConcludeBattleDto(
        Long battleId,
        String winnerFaction,
        SurvivingUnitsDto[] survivingUnits,
        RpCharCasualtyDto[] playersKilled
) {
}
