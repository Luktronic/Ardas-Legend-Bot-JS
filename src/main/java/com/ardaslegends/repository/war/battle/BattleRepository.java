package com.ardaslegends.repository.war.battle;

import com.ardaslegends.domain.war.battle.Battle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleRepository extends JpaRepository<Battle, Long>, BattleRepositoryCustom {
}
