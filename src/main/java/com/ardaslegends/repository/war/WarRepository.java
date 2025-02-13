package com.ardaslegends.repository.war;

import com.ardaslegends.domain.Faction;
import com.ardaslegends.domain.war.War;
import com.ardaslegends.domain.war.WarParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface WarRepository extends JpaRepository<War, Long>, WarRepositoryCustom {
    public Set<War> findByName(String name);

    @Query("""
            select w from War w 
                left join w.aggressors aggressors 
                left join w.defenders defenders
            where (aggressors.warParticipant = ?1
            or defenders.warParticipant = ?1)
            and isActive = true""")
    Set<War> findAllActiveWarsWithFaction(Faction faction);
}
