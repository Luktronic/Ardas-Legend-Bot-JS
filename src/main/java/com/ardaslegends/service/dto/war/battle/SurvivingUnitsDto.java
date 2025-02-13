package com.ardaslegends.service.dto.war.battle;

import com.ardaslegends.service.dto.unit.UnitTypeDto;

public record SurvivingUnitsDto(
        String army,
        UnitTypeDto[] survivingUnits
) {
}
