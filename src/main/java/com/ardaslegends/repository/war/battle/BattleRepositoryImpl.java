package com.ardaslegends.repository.war.battle;

import com.ardaslegends.domain.war.battle.Battle;
import com.ardaslegends.domain.war.battle.QBattle;
import com.ardaslegends.repository.exceptions.NotFoundException;
import com.ardaslegends.repository.exceptions.RepositoryNullPointerException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

@Slf4j
public class BattleRepositoryImpl extends QuerydslRepositorySupport implements BattleRepositoryCustom {


    public BattleRepositoryImpl() { super(Battle.class); }

    @Override
    public Battle queryByIdOrElseThrow(Long id) {
        log.debug("Finding battle by id [{}]", id);
        if(id == null) {
            log.warn("Id was null in BattleRepositoryImpl.findByIdOrElseThrow");
            throw RepositoryNullPointerException.queryMethodParameterWasNull("id", "findByIdOrElseThrow");
        }

        QBattle qBattle = QBattle.battle;

        val result = from(qBattle)
                .where(qBattle.id.eq(id))
                .fetchFirst();

        if(result == null) {
            log.warn("Could not find battle with id [{}]", id);
            throw NotFoundException.genericNotFound("battle", "id", id.toString());
        }

        log.debug("Found battle [{}]", result);
        return result;
    }
}
