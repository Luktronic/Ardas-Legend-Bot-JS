package com.ardaslegends.repository.claimbuild;

import com.ardaslegends.domain.ClaimBuild;
import com.ardaslegends.domain.Faction;

import java.util.List;
import java.util.Optional;

public interface ClaimbuildRepositoryCustom {
    ClaimBuild queryByNameIgnoreCase(String claimbuildName);
    Optional<ClaimBuild> queryByNameIgnoreCaseOptional(String claimbuildName);
    boolean existsByNameIgnoreCase(String claimbuildName);
    List<ClaimBuild> queryByNames(String[] names);
    List<ClaimBuild> queryByFaction(Faction faction);

}
