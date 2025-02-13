package com.ardaslegends.repository.war.battle;

import com.ardaslegends.domain.war.battle.Battle;

public interface BattleRepositoryCustom {

    Battle queryByIdOrElseThrow(Long id);
}
