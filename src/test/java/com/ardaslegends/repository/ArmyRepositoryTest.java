package com.ardaslegends.repository;

import com.ardaslegends.domain.*;
import com.ardaslegends.repository.faction.FactionRepository;
import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.sql.init.mode=never"})
@ActiveProfiles("test")
public class ArmyRepositoryTest {

    @Autowired
    ArmyRepository armyRepository;
    @Autowired
    FactionRepository factionRepository;

    private Faction gondor;
    private Region region1;
    private RPChar rpChar;
    private ClaimBuild claimbuild;
    private Army army;
    private Unit gondorSoldierUnit;
    private Unit gondorArcherUnit;

    @BeforeEach
    void setup() {
        region1 = Region.builder().name("").id("1").regionType(RegionType.DESERT).build();
        val region2 = Region.builder().name("").id("2").regionType(RegionType.DESERT).build();
        val region3 = Region.builder().name("").id("3").regionType(RegionType.DESERT).build();
        val region4 = Region.builder().name("").id("4").regionType(RegionType.DESERT).build();
        val region5 = Region.builder().name("").id("5").regionType(RegionType.DESERT).build();


        gondor = Faction.builder().name("Gondor").homeRegion(region1).build();
        rpChar = RPChar.builder().name("Belegorn").currentRegion(region1).build();
        var mordor = Faction.builder().name("Mordor").homeRegion(region2).build();
        claimbuild = ClaimBuild.builder().name("Nimheria").coordinates(new Coordinate(0,0,0)).ownedBy(gondor)
                .region(region1).type(ClaimBuildType.CASTLE).build();

        factionRepository.saveAll(List.of(gondor, mordor));

        army = new Army("Army", ArmyType.ARMY, gondor, region1, rpChar, new ArrayList<>(),
                new ArrayList<>(), null, 0.0, false, null, null, 0, 0, claimbuild,
                OffsetDateTime.now(), true);

        val gondorSoldier = new UnitType("Gondor Soldier", 1.0);
        val gondorArcher = new UnitType("Gondor Archer", 1.5);

        gondorSoldierUnit = new Unit(null, gondorSoldier, army, 21, 21, false);
        gondorArcherUnit = new Unit(null, gondorArcher, army, 6, 6, false);
        val units = new ArrayList<Unit>();
        units.add(gondorSoldierUnit);
        units.add(gondorArcherUnit);
        army.setUnits(units);
    }

    @Test
    void ensureSaveArmyWorks() {

        val result = armyRepository.save(army);

        assertThat(result).isNotNull();
    }

    @Test
    void ensureUnitsAreSavedWhenArmyIsSaved() {

        val result = armyRepository.save(army);

        var soldier = result.getUnits().stream().filter(unit -> unit.equals(gondorSoldierUnit)).findFirst().get();
        var archer = result.getUnits().stream().filter(unit -> unit.equals(gondorArcherUnit)).findFirst().get();
        assertThat(soldier.getAmountAlive()).isEqualTo(gondorSoldierUnit.getAmountAlive());
        assertThat(archer.getAmountAlive()).isEqualTo(gondorArcherUnit.getAmountAlive());

        gondorSoldierUnit.setAmountAlive(5);
        gondorArcherUnit.setAmountAlive(0);
        val units = new ArrayList<Unit>();
        units.add(gondorSoldierUnit);
        units.add(gondorArcherUnit);
        result.setUnits(units);
        result.setFreeTokens(25.0);

        armyRepository.save(result);

        val armyOptional = armyRepository.findArmyByName(result.getName());
        assertThat(armyOptional.isPresent()).isTrue();
        val fetchedArmy = armyOptional.get();
        soldier = fetchedArmy.getUnits().stream().filter(unit -> unit.equals(gondorSoldierUnit)).findFirst().get();
        archer = fetchedArmy.getUnits().stream().filter(unit -> unit.equals(gondorArcherUnit)).findFirst().get();
        assertThat(soldier.getAmountAlive()).isEqualTo(5);
        assertThat(archer.getAmountAlive()).isEqualTo(0);
        assertThat(fetchedArmy.getFreeTokens()).isEqualTo(25.0);
    }
}
