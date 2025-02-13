package com.ardaslegends.service;

import com.ardaslegends.domain.*;
import com.ardaslegends.repository.ArmyRepository;
import com.ardaslegends.repository.MovementRepository;
import com.ardaslegends.repository.player.PlayerRepository;
import com.ardaslegends.service.time.ScheduleService;
import com.ardaslegends.service.time.TimeFreezeService;
import com.ardaslegends.service.utils.ServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ScheduleServiceTest {

    private ScheduleService scheduleService;
    private MovementRepository mockMovementRepository;
    private ArmyRepository mockArmyRepository;
    private PlayerRepository mockPlayerRepository;
    private MovementService mockMovementService;
    private ArmyService mockArmyService;
    private PlayerService mockPlayerService;
    private TimeFreezeService mockTimeFreezeService;
    private Clock mockClock;
    private Clock fixedClock;

    private Player player;
    private Player player2;
    private RPChar rpChar;
    private RPChar rpChar2;
    private Army army;
    private Army army2;
    private Region region;
    private Region region2;
    private Region region3;
    private Region region4;
    private PathElement pathElement;
    private PathElement pathElement2;
    private PathElement pathElement3;
    private PathElement pathElement4;
    private List<PathElement> path;
    private List<PathElement> path2;
    private List<PathElement> path3;
    private Movement movement;
    private Movement movement2;
    private Movement movement3;
    private ClaimBuild claimBuild;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private OffsetDateTime endTime2;
    private OffsetDateTime endTime3;
    private UnitType unitType;
    private UnitType unitType2;
    private UnitType unitType3;
    private Unit unit;
    private Unit unit2;
    private Unit unit3;

    @BeforeEach
    void setup() {
        mockMovementRepository = mock(MovementRepository.class);
        mockArmyRepository = mock(ArmyRepository.class);
        mockPlayerRepository = mock(PlayerRepository.class);
        mockMovementService = mock(MovementService.class);
        mockArmyService = mock(ArmyService.class);
        mockPlayerService = mock(PlayerService.class);
        mockClock = mock(Clock.class);
        mockTimeFreezeService = mock(TimeFreezeService.class);

        scheduleService = new ScheduleService(mockMovementRepository, mockArmyRepository, mockPlayerRepository, mockMovementService, mockArmyService, mockPlayerService, mockTimeFreezeService, mockClock);

        unitType = UnitType.builder().unitName("Gondor Soldier").tokenCost(1.0).build();
        unitType2 = UnitType.builder().unitName("Gondor Archer").tokenCost(1.5).build();
        unitType3 = UnitType.builder().unitName("Tower Guard").tokenCost(2.0).build();
        unit = Unit.builder().unitType(unitType).amountAlive(2).count(4).isMounted(false).build();
        unit2 = Unit.builder().unitType(unitType2).amountAlive(2).count(5).isMounted(false).build();
        unit3 = Unit.builder().unitType(unitType3).amountAlive(2).count(4).isMounted(false).build();
        region = Region.builder().id("91").regionType(RegionType.LAND).build();
        region2 = Region.builder().id("92").regionType(RegionType.LAND).build();
        region3 = Region.builder().id("93").regionType(RegionType.LAND).build();
        region4 = Region.builder().id("94").regionType(RegionType.LAND).build();
        rpChar = RPChar.builder().name("Belegorn").boundTo(army).currentRegion(region).build();
        rpChar2 = RPChar.builder().name("Tinwe").currentRegion(region4).build();
        player = Player.builder().build();
        player.addActiveRpChar(rpChar);
        player2 = Player.builder().build();
        player2.addActiveRpChar(rpChar2);
        army = Army.builder().name("Knights of Gondor").currentRegion(region).boundTo(player.getActiveCharacter().get()).build();
        army2 = Army.builder().name("Gondor Army").currentRegion(region2).boundTo(null).build();
        pathElement = PathElement.builder().region(region).actualCost(region.getCost()).baseCost(region.getCost()).build();
        pathElement2 = PathElement.builder().region(region2).actualCost(region2.getCost()).baseCost(region2.getCost()).build();
        pathElement3 = PathElement.builder().region(region3).actualCost(region3.getCost()).baseCost(region3.getCost()).build();
        pathElement4 = PathElement.builder().region(region4).actualCost(region4.getCost()).baseCost(region4.getCost()).build();
        path = List.of(pathElement, pathElement2, pathElement3, pathElement4);
        path2 = List.of(pathElement4, pathElement3, pathElement2);
        path3 = List.of(pathElement2, pathElement3);
        startTime = OffsetDateTime.now();
        endTime = startTime.plusHours(ServiceUtils.getTotalPathCost(path));
        endTime2 = startTime.plusHours(ServiceUtils.getTotalPathCost(path2));
        endTime3 = startTime.plusHours(ServiceUtils.getTotalPathCost(path3));
        movement = Movement.builder().isCharMovement(false).isCurrentlyActive(true).army(army).rpChar(player.getActiveCharacter().get()).path(path)
                .startTime(startTime).endTime(endTime).reachesNextRegionAt(startTime.plusHours(path.get(1).getActualCost()))
                .build();
        movement2 = Movement.builder().isCharMovement(true).isCurrentlyActive(true).army(null).rpChar(player2.getActiveCharacter().get()).path(path2)
                .startTime(startTime).endTime(endTime2).reachesNextRegionAt(startTime.plusHours(path2.get(1).getActualCost()))
                .build();
        movement3 = Movement.builder().isCharMovement(false).isCurrentlyActive(true).army(army2).rpChar(null).path(path3)
                .startTime(startTime).endTime(endTime3).reachesNextRegionAt(startTime.plusHours(path3.get(1).getActualCost()))
                .build();
        claimBuild = ClaimBuild.builder().type(ClaimBuildType.CASTLE).specialBuildings(List.of(SpecialBuilding.HOUSE_OF_HEALING)).region(region).build();

        when(mockMovementRepository.findMovementsByIsCurrentlyActive(true)).thenReturn(List.of(movement, movement2, movement3));
        fixedClock = Clock.fixed(startTime.plusDays(1).plusSeconds(1).toInstant(), ZoneId.systemDefault());
        when(mockClock.instant()).thenReturn(fixedClock.instant());
        when(mockClock.getZone()).thenReturn(fixedClock.getZone());
    }

    @Test
    void ensureHandleMovementsWorksForArmyMoves() {
        log.debug("Testing if handleMovements works properly!");

        //This is so we check if the bot instantly completes movements that are already over the end date
        movement3.setStartTime(startTime.minusMonths(1));
        movement3.setEndTime(startTime.minusMonths(1).plusHours(ServiceUtils.getTotalPathCost(path3)));

        List<Movement> movements = List.of(movement, movement3);
        when(mockMovementRepository.findMovementsByIsCurrentlyActive(true)).thenReturn(movements);
        when(mockMovementService.saveMovements(movements)).thenReturn(movements);

        scheduleService.handleMovements();

        assertThat(army.getCurrentRegion()).isEqualTo(region2);
        assertThat(rpChar.getCurrentRegion()).isEqualTo(region2);
        assertThat(army2.getCurrentRegion()).isEqualTo(region3);
        log.info("Test passed: handleMovements works properly!");
    }

    @Test
    void ensureHandleMovementsWorksForCharMoves() {
        log.debug("Testing if handleMovements works properly!");

        pathElement.setActualCost(pathElement.getActualCost()/2);
        pathElement2.setActualCost(pathElement2.getActualCost()/2);
        pathElement3.setActualCost(pathElement3.getActualCost()/2);
        pathElement4.setActualCost(pathElement4.getActualCost()/2);
        endTime2 = startTime.plusHours(ServiceUtils.getTotalPathCost(path2));
        movement2.setStartTime(startTime);
        movement2.setEndTime(endTime2);
        movement2.setReachesNextRegionAt(startTime.plusHours(pathElement3.getActualCost()));

        List<Movement> movements = List.of(movement2);
        when(mockMovementRepository.findMovementsByIsCurrentlyActive(true)).thenReturn(movements);
        when(mockMovementService.saveMovements(movements)).thenReturn(movements);

        scheduleService.handleMovements();

        assertThat(rpChar2.getCurrentRegion()).isEqualTo(region2);
        log.info("Test passed: handleMovements works properly!");
    }

    @Test
    void ensureHandleMovementsExitsWhenHourHasNotPassed() {
        log.debug("Testing if handleMovements works properly!");

        fixedClock = Clock.fixed(startTime.plusMinutes(50).toInstant(), ZoneId.systemDefault());
        when(mockClock.instant()).thenReturn(fixedClock.instant());
        when(mockClock.getZone()).thenReturn(fixedClock.getZone());

        List<Movement> movements = List.of(movement, movement3);
        when(mockMovementRepository.findMovementsByIsCurrentlyActive(true)).thenReturn(movements);
        when(mockMovementService.saveMovements(movements)).thenReturn(movements);

        scheduleService.handleMovements();

        assertThat(army.getCurrentRegion()).isEqualTo(path.get(0).getRegion());
        assertThat(rpChar.getCurrentRegion()).isEqualTo(path.get(0).getRegion());
        assertThat(army2.getCurrentRegion()).isEqualTo(path3.get(0).getRegion());
        log.info("Test passed: handleMovements works properly!");
    }

    @Test
    void ensureHandleHealingWorksForChars() {
        log.debug("Testing if handleHealings works properly for rp chars!");

        rpChar.setInjured(true);
        rpChar.setIsHealing(true);
        rpChar.setCurrentRegion(region);
        rpChar.setStartedHeal(startTime);
        rpChar.setHealEnds(startTime.plusDays(2));

        rpChar2.setInjured(true);
        rpChar2.setIsHealing(true);
        rpChar2.setCurrentRegion(region);
        rpChar2.setStartedHeal(startTime.minusDays(1));
        rpChar2.setHealEnds(startTime.plusDays(1));

        log.info("Current time: [{}] - start time: [{}] - end time: [{}]", OffsetDateTime.now(mockClock), startTime.minusDays(1), startTime.plusDays(1));

        List<Player> players = List.of(player, player2);
        when(mockPlayerRepository.queryPlayersWithHealingRpchars()).thenReturn(players);
        when(mockPlayerService.savePlayers(players)).thenReturn(players);

        scheduleService.handleHealings();

        assertThat(rpChar.getIsHealing()).isTrue();
        assertThat(rpChar.getInjured()).isTrue();
        assertThat(rpChar2.getIsHealing()).isFalse();
        assertThat(rpChar2.getInjured()).isFalse();
        log.info("Test passed: handleHealings works properly for rp chars!");
    }

    @Test
    void ensureHealingWorksForArmies() {
        log.debug("Testing if healing armies works");

        army.setUnits(List.of(unit3, unit2, unit));
        army.setStationedAt(claimBuild);
        army.setIsHealing(true);
        army.setHealStart(startTime);
        army.setHealEnd(startTime.plusHours(army.getAmountOfHealHours()));
        army.setHoursHealed(0);
        army.setHoursLeftHealing(army.getAmountOfHealHours());

        List<Army> armies = List.of(army);
        when(mockArmyRepository.findArmyByIsHealingTrue()).thenReturn(armies);
        when(mockArmyService.saveArmies(armies)).thenReturn(armies);

        scheduleService.handleHealings();

        assertThat(unit.getAmountAlive()).isEqualTo(unit.getCount());
        assertThat(unit2.getAmountAlive()).isEqualTo(4);
        assertThat(unit3.getAmountAlive()).isEqualTo(2);
        log.info("Test passed: healing armies works");
    }

    @Test
    void ensureHealingWorksForArmiesWhenHealingIsDone() {
        log.debug("Testing if healing armies works when healing is done");


        army.setUnits(List.of(unit3, unit2, unit));
        army.setStationedAt(claimBuild);
        army.setIsHealing(true);
        army.setHealStart(startTime.minusDays(3));
        army.setHealEnd(startTime.minusDays(3).plusHours(army.getAmountOfHealHours()));
        army.setHoursHealed(0);
        army.setHoursLeftHealing(army.getAmountOfHealHours());
        log.info("Army needs to heal for [{}] hours", army.getAmountOfHealHours());

        List<Army> armies = List.of(army);
        when(mockArmyRepository.findArmyByIsHealingTrue()).thenReturn(armies);
        when(mockArmyService.saveArmies(armies)).thenReturn(armies);

        scheduleService.handleHealings();

        assertThat(unit.getAmountAlive()).isEqualTo(unit.getCount());
        assertThat(unit2.getAmountAlive()).isEqualTo(unit2.getCount());
        assertThat(unit3.getAmountAlive()).isEqualTo(unit3.getCount());
        assertThat(army.getIsHealing()).isFalse();
        assertThat(army.getHealStart()).isNull();
        assertThat(army.getHealEnd()).isNull();
        assertThat(army.getHoursHealed()).isEqualTo(0);
        assertThat(army.getHoursLeftHealing()).isEqualTo(0);
        assertThat(army.allUnitsAlive()).isTrue();
        log.info("Test passed: healing armies works when healing is done");
    }

    @Test
    void ensureHealingIsDoubledInStrongholds() {
        log.debug("Testing if healing armies is double the speed when healing in strongholds");

        unit3.setIsMounted(true);

        army.setUnits(List.of(unit3, unit2, unit));
        claimBuild.setType(ClaimBuildType.STRONGHOLD);
        army.setStationedAt(claimBuild);
        army.setIsHealing(true);
        army.setHealStart(startTime.minusDays(1));
        army.setHealEnd(startTime.minusDays(1).plusHours(army.getAmountOfHealHours()));
        army.setHoursHealed(0);
        army.setHoursLeftHealing(army.getAmountOfHealHours());

        List<Army> armies = List.of(army);
        when(mockArmyRepository.findArmyByIsHealingTrue()).thenReturn(armies);
        when(mockArmyService.saveArmies(armies)).thenReturn(armies);

        scheduleService.handleHealings();

        assertThat(unit.getAmountAlive()).isEqualTo(unit.getCount());
        assertThat(unit2.getAmountAlive()).isEqualTo(unit2.getCount());
        assertThat(unit3.getAmountAlive()).isEqualTo(unit3.getCount());
        log.info("Test passed: healing armies is double the speed when healing in strongholds");
    }

    @Test
    void ensureHealingArmiesDoesNothingWhenNoHourHasPassed() {
        log.debug("Testing if healing armies does nothing when no hour has passed");

        int before1 = unit.getAmountAlive();
        int before2 = unit2.getAmountAlive();
        int before3 = unit3.getAmountAlive();

        army.setUnits(List.of(unit3, unit2, unit));
        army.setStationedAt(claimBuild);
        army.setIsHealing(true);
        army.setHealStart(startTime);
        army.setHealEnd(startTime.plusHours(army.getAmountOfHealHours()));
        army.setHoursHealed(0);
        army.setHoursLeftHealing(army.getAmountOfHealHours());

        List<Army> armies = List.of(army);
        when(mockArmyRepository.findArmyByIsHealingTrue()).thenReturn(armies);
        when(mockArmyService.saveArmies(armies)).thenReturn(armies);
        fixedClock = Clock.fixed(startTime.plusMinutes(50).toInstant(), ZoneId.systemDefault());
        when(mockClock.instant()).thenReturn(fixedClock.instant());
        when(mockClock.getZone()).thenReturn(fixedClock.getZone());
        scheduleService.handleHealings();

        assertThat(unit.getAmountAlive()).isEqualTo(before1);
        assertThat(unit2.getAmountAlive()).isEqualTo(before2);
        assertThat(unit3.getAmountAlive()).isEqualTo(before3);
        log.info("Test passed: healing armies does nothing when no hour has passed");
    }

}
