package com.ardaslegends.repository.war;

import com.ardaslegends.domain.Faction;
import com.ardaslegends.domain.war.War;

import java.util.Optional;
import java.util.Set;

public interface WarRepositoryCustom {
    Set<War> queryWarsByFaction(Faction faction, QueryWarStatus warStatus);
    Set<War> queryWarsBetweenFactions(Faction faction1, Faction faction2, QueryWarStatus warStatus);
    Optional<War> queryActiveInitialWarBetween(Faction faction1, Faction faction2);
    Optional<War> queryActiveWarByName(String name);
}
