package com.ardaslegends.service;

import com.ardaslegends.domain.*;
import com.ardaslegends.domain.war.battle.Battle;
import com.ardaslegends.domain.war.battle.BattleLocation;
import com.ardaslegends.domain.war.War;
import com.ardaslegends.domain.war.WarParticipant;
import com.ardaslegends.repository.war.battle.BattleRepository;
import com.ardaslegends.repository.war.WarRepository;
import com.ardaslegends.service.dto.war.battle.CreateBattleDto;
import com.ardaslegends.service.discord.DiscordService;
import com.ardaslegends.service.exceptions.logic.army.ArmyServiceException;
import com.ardaslegends.service.exceptions.logic.war.BattleServiceException;
import com.ardaslegends.service.time.TimeFreezeService;
import com.ardaslegends.service.time.Timer;
import com.ardaslegends.service.war.BattleService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class BattleServiceTest {
    private WarRepository mockWarRepository;
    private BattleService battleService;

    private Faction faction1;
    private Faction faction2;

    private RPChar rpchar2;

    private Player player1;

    private Army army1;
    private Army army2;

    private War war;
    private  WarParticipant warParticipant2;

    private BattleLocation battleLocation;

    private Region region1;
    private Region region2;
    private ClaimBuild claimBuild2;
    private Movement movement;

    Set<Army> attackingArmies;
    Set<Army> defendingArmies;

    CreateBattleDto createBattleDto;
    @BeforeEach
    void setup(){
        BattleRepository mockBattleRepository = mock(BattleRepository.class);
        mockWarRepository = mock(WarRepository.class);
        Pathfinder pathfinder = mock(Pathfinder.class);
        ArmyService mockArmyService = mock(ArmyService.class);
        PlayerService mockPlayerService = mock(PlayerService.class);
        ClaimBuildService mockClaimBuildService = mock(ClaimBuildService.class);
        RpCharService mockRpCharService = mock(RpCharService.class);
        TimeFreezeService mockTimeFreezeService = mock(TimeFreezeService.class);
        DiscordService mockDiscordService = mock(DiscordService.class);
        FactionService mockFactionService = mock(FactionService.class);
        User mockUser1 = mock(User.class);
        User mockUser2 = mock(User.class);
        Role mockRole = mock(Role.class);
        battleService = new BattleService(mockBattleRepository, mockArmyService, mockPlayerService, mockRpCharService, mockClaimBuildService,mockWarRepository, pathfinder, mockFactionService, mockTimeFreezeService, mockDiscordService);

        region1 = Region.builder().id("90").neighboringRegions(new HashSet<>()).regionType(RegionType.LAND).build();
        region2 = Region.builder().id("91").neighboringRegions(new HashSet<>()).regionType(RegionType.HILL).build();
        Region region3 = Region.builder().id("92").neighboringRegions(new HashSet<>()).regionType(RegionType.LAND).build();

        region1.addNeighbour(region2);
        region2.addNeighbour(region1);
        region2.addNeighbour(region3);
        region3.addNeighbour(region2);

        faction1 = Faction.builder().name("Gondor").allies(new ArrayList<>()).factionRoleId(1L).foodStockpile(10).build();
        faction2 = Faction.builder().name("Isengard").allies(new ArrayList<>()).factionRoleId(2L).foodStockpile(10).build();

        ClaimBuild claimBuild1 = ClaimBuild.builder().name("Nimheria").type(ClaimBuildType.CAPITAL).siege("Ram, Trebuchet, Tower").region(region1).ownedBy(faction1).specialBuildings(List.of(SpecialBuilding.HOUSE_OF_HEALING)).stationedArmies(List.of()).build();
        claimBuild2 = ClaimBuild.builder().name("Aira").type(ClaimBuildType.TOWN).siege("Ram, Trebuchet, Tower").region(region2).ownedBy(faction2).specialBuildings(List.of(SpecialBuilding.HOUSE_OF_HEALING)).stationedArmies(List.of()).build();
        ClaimBuild claimBuild3 = ClaimBuild.builder().type(ClaimBuildType.STRONGHOLD).name("Dondle").siege("Ram, Trebuchet, Tower").region(region3).ownedBy(faction2).specialBuildings(List.of(SpecialBuilding.HOUSE_OF_HEALING)).stationedArmies(List.of()).build();

        UnitType unitType1 = UnitType.builder().unitName("Gondor Archer").tokenCost(1.5).build();
        UnitType unitType2 = UnitType.builder().unitName("Isengard Archer").tokenCost(1.5).build();

        Unit unit1 = Unit.builder().unitType(unitType1).army(army1).amountAlive(5).count(10).build();
        Unit unit2 = Unit.builder().unitType(unitType2).army(army2).amountAlive(5).count(10).build();

        RPChar rpchar1 = RPChar.builder().name("Belegorn").isHealing(false).currentRegion(region1).build();
        rpchar1.setId(1L);
        rpchar2 = RPChar.builder().name("Gandalf").isHealing(false).currentRegion(region2).build();
        rpchar2.setId(2L);

        player1 = Player.builder().discordID("1234").ign("Luk").faction(faction1).build();
        player1.addActiveRpChar(rpchar1);
        rpchar1.setOwner(player1);
        Player player2 = Player.builder().discordID("4321").ign("mirak").faction(faction2).build();
        rpchar2.setOwner(player2);
        player2.addActiveRpChar(rpchar2);

        army1 = Army.builder().name("Knights of Gondor").armyType(ArmyType.ARMY).faction(faction1).freeTokens(30 - unit1.getCount() * unitType1.getTokenCost()).currentRegion(region2).boundTo(player1.getActiveCharacter().get()).stationedAt(claimBuild1).sieges(new ArrayList<>()).createdAt(OffsetDateTime.now().minusDays(3)).build();
        army2 = Army.builder().name("Knights of Isengard").armyType(ArmyType.ARMY).faction(faction2).freeTokens(30 - unit2.getCount() * unitType2.getTokenCost()).currentRegion(region2).boundTo(player2.getActiveCharacter().get()).stationedAt(claimBuild2).sieges(new ArrayList<>()).createdAt(OffsetDateTime.now().minusDays(3)).build();

        army2.setMovements(new ArrayList<>());
        army1.setMovements(new ArrayList<>());
        WarParticipant warParticipant1 = WarParticipant.builder().warParticipant(faction1).initialParty(true).joiningDate(OffsetDateTime.now()).build();
        warParticipant1 = WarParticipant.builder().warParticipant(faction2).initialParty(true).joiningDate(OffsetDateTime.now()).build();

        Set<WarParticipant> attacker = new HashSet<>();
        Set<WarParticipant> defender = new HashSet<>();

        attacker.add(warParticipant1);
        defender.add(warParticipant2);

        war = War.builder().name("War of Gondor").aggressors(attacker).defenders(defender).startDate(OffsetDateTime.now()).build();

        attackingArmies = new HashSet<>();
        attackingArmies.add(army1);
        defendingArmies = new HashSet<>();
        defendingArmies.add(army2);

        battleLocation = new BattleLocation(region2,true, null);

        Battle battle = new Battle(new HashSet<>(Set.of(war)), "Battle of Gondor", attackingArmies, defendingArmies, OffsetDateTime.now(), OffsetDateTime.of(2023, 9, 20, 0, 0, 0, 0, ZoneOffset.UTC), OffsetDateTime.of(2023, 9, 30, 0, 0, 0, 0, ZoneOffset.UTC), OffsetDateTime.of(2023, 9, 20, 0, 0, 0, 0, ZoneOffset.UTC), battleLocation);

        PathElement pathElement1 = PathElement.builder().region(region1).baseCost(region1.getCost()).actualCost(0).build();
        PathElement pathElement2 = PathElement.builder().region(region2).baseCost(region2.getCost()).actualCost(region2.getCost()).build();
        PathElement pathElement3 = PathElement.builder().region(region3).baseCost(region3.getCost()).actualCost(region3.getCost()).build();
        List<PathElement> path = List.of(pathElement2, pathElement1);

        val now = OffsetDateTime.now();
        movement =  Movement.builder().isCharMovement(false).startTime(now.minusHours(2)).reachesNextRegionAt(now.plusHours(46)).endTime(now.plusHours(46)).isCurrentlyActive(true).army(army1).path(path).build();

        createBattleDto = new CreateBattleDto("1234","Battle of Gondor","Knights of Gondor",
                "Knights of Isengard",true,null);

        when(mockPlayerService.getPlayerByDiscordId(any())).thenReturn(player1);
        when(mockArmyService.getArmyByName(any())).thenReturn(army1);
        when(mockPlayerService.getPlayerByDiscordId(player1.getDiscordID())).thenReturn(player1);
        when(mockPlayerService.getPlayerByDiscordId(player2.getDiscordID())).thenReturn(player2);
        when(mockDiscordService.getUserById(player1.getDiscordID())).thenReturn(mockUser1);
        when(mockDiscordService.getUserById(player2.getDiscordID())).thenReturn(mockUser2);
        when(mockDiscordService.getRoleByIdOrElseThrow(anyLong())).thenReturn(mockRole);
        when(mockUser1.getMentionTag()).thenReturn(player1.getIgn());
        when(mockUser2.getMentionTag()).thenReturn(player2.getIgn());
        when(mockRole.getMentionTag()).thenReturn("RoleMentionTag");
        when(pathfinder.findShortestWay(army1.getCurrentRegion(), region2,player1,false)).thenReturn(movement.getPath());
        when(mockClaimBuildService.getClaimBuildByName(claimBuild1.getName())).thenReturn(claimBuild1);
        when(mockClaimBuildService.getClaimBuildByName(claimBuild2.getName())).thenReturn(claimBuild2);
        when(mockClaimBuildService.getClaimBuildByName(claimBuild3.getName())).thenReturn(claimBuild3);
        when(mockWarRepository.queryActiveInitialWarBetween(any(),any())).thenReturn(Optional.of(war));
        when(mockWarRepository.queryWarsBetweenFactions(any(),any(), any())).thenReturn(Set.of(war));
        when(mockBattleRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(mockArmyService.getArmyByName("Knights of Gondor")).thenReturn(army1);
        when(mockArmyService.getArmyByName("Knights of Isengard")).thenReturn(army2);
        when(mockTimeFreezeService.start24hTimer(any(Callable.class))).thenReturn(new Timer<>(CompletableFuture.completedFuture(battle), OffsetDateTime.now().plusHours(24)));
    }

    @Test
    void ensureCreateBattleWorksWhenPlayerBoundToArmy(){
        log.debug("Testing if createBattle works when player is not leader but bound to the army!");

        // Assign
        log.trace("Initializing player, rpchar, regions, army");
        CreateBattleDto createBattleDto = new CreateBattleDto("1234","Battle of Gondor","Knights of Gondor","Knights of Isengard",true, null);

        Battle newBattle = battleService.createBattle(createBattleDto);
        log.debug(newBattle.getName());
        assertThat(newBattle).isNotNull();
        assertThat(newBattle.getName()).isEqualTo("Battle of Gondor");
        assertThat(newBattle.getWars()).contains(war);
        assertThat(newBattle.getAttackingArmies()).isEqualTo(attackingArmies);
        assertThat(newBattle.getDefendingArmies()).isEqualTo(defendingArmies);
        assertThat(newBattle.getBattleLocation()).isEqualTo(battleLocation);
    }

    @Test
    void ensureCreateBattleWorksWithClaimBuildBattle(){
        log.debug("Testing if createBattle works when player is not leader but bound to the army!");

        // Assign
        log.trace("Initializing player, rpchar, regions, army");
        claimBuild2.setStationedArmies(List.of(army2));
        army2.setStationedAt(claimBuild2);
        battleLocation = new BattleLocation(claimBuild2.getRegion(), false, claimBuild2);
        CreateBattleDto createBattleDto = new CreateBattleDto("1234","Battle of Gondor","Knights of Gondor",null,false, claimBuild2.getName());

        Battle newBattle = battleService.createBattle(createBattleDto);
        log.debug(newBattle.getName());
        assertThat(newBattle).isNotNull();
        assertThat(newBattle.getName()).isEqualTo("Battle of Gondor");
        assertThat(newBattle.getWars()).contains(war);
        assertThat(newBattle.getAttackingArmies()).isEqualTo(attackingArmies);
        assertThat(newBattle.getBattleLocation()).isEqualTo(battleLocation);
    }

    @Test
    void ensureCreateBattleWorksWhenPlayerIsLeader(){
        log.debug("Testing if createBattle works when player is the faction leader but not bound to the army!");
        army1.setBoundTo(rpchar2);
        faction1.setLeader(player1);

        Battle newBattle = battleService.createBattle(createBattleDto);
        log.debug(newBattle.getName());
        assertThat(newBattle).isNotNull();
        assertThat(newBattle.getName()).isEqualTo("Battle of Gondor");
        assertThat(newBattle.getWars()).contains(war);
        assertThat(newBattle.getAttackingArmies()).isEqualTo(attackingArmies);
        assertThat(newBattle.getDefendingArmies()).isEqualTo(defendingArmies);
    }

    @Test
    void ensureCreateBattleWorksWhenDefendingArmyIsMovingButCanBeCaught(){
        log.debug("Testing if createBattle works when defending army is moving but can be caught!");

        // Assign
        log.trace("Initializing player, rpchar, regions, army");
        CreateBattleDto createBattleDto = new CreateBattleDto("1234","Battle of Gondor","Knights of Gondor","Knights of Isengard",true,"Aira");

        Battle newBattle = battleService.createBattle(createBattleDto);
        log.debug(newBattle.getName());
        assertThat(newBattle).isNotNull();
        assertThat(newBattle.getName()).isEqualTo("Battle of Gondor");
        assertThat(newBattle.getWars()).contains(war);
        assertThat(newBattle.getAttackingArmies()).isEqualTo(attackingArmies);
        assertThat(newBattle.getDefendingArmies()).isEqualTo(defendingArmies);
    }

    @Test
    void ensureCreateBattleThrowsExceptionWhenPlayerNotBound(){
        //Player1 is not bound to attacking army, so they have no permission to create a battle
        CreateBattleDto createBattleDto = new CreateBattleDto(player1.getDiscordID(),"Battle of Gondor",
                "Knights of Isengard","Knights of Gondor",true,"Aira");

        var exception = assertThrows(ArmyServiceException.class, ()-> battleService.createBattle(createBattleDto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.noPermissionToPerformThisAction().getMessage());
    }

    @Test
    void ensureCreateBattleThrowsNotEnoughHealthException(){
        army1.setFreeTokens(0.0);

        CreateBattleDto createBattleDto = new CreateBattleDto("1234","Battle of Gondor","Knights of Gondor","Knights of Isengard",true,"Aira");

        var exception = assertThrows(BattleServiceException.class, ()-> battleService.createBattle(createBattleDto));

        assertThat(exception.getMessage()).contains("Army does not have enough health");
    }

    @Test
    void ensureCreateBattleThrowsExceptionWhenDefendingArmyIsMovingAway(){
        log.debug("Testing if createBattle throws exception when defending army is moving away and cannot be caught!");
        
        movement.setEndTime(movement.getStartTime().plusHours(22));
        movement.setReachesNextRegionAt(movement.getStartTime().plusHours(22));
        army2.getMovements().add(movement);

        var exception = assertThrows(BattleServiceException.class, ()-> battleService.createBattle(createBattleDto));

        assertThat(exception.getMessage()).isEqualTo(BattleServiceException.defendingArmyIsMovingAway(army2).getMessage());
    }

    @Test
    void ensureCreateBattleThrowsExceptionWhenFactionsNotAtWar(){
        log.debug("Testing if createBattle throws exception when factions are not at war!");

        war = null;
        when(mockWarRepository.queryWarsBetweenFactions(any(),any(),any())).thenReturn(new HashSet<>());


        var exception = assertThrows(BattleServiceException.class, ()-> battleService.createBattle(createBattleDto));

        assertThat(exception.getMessage()).isEqualTo(BattleServiceException.factionsNotAtWar(faction1.getName(), faction2.getName()).getMessage());
    }

    @Test
    void ensureCreateBattleThrowsExceptionWhenAttackingArmyIsMoving(){
        log.debug("Testing if createBattle throws exception when attacking army is moving!");

        movement.setArmy(army1);
        var movements = new ArrayList<Movement>();
        movements.add(movement);
        army1.setMovements(movements);

        var exception = assertThrows(BattleServiceException.class, ()-> battleService.createBattle(createBattleDto));

        assertThat(exception.getMessage()).isEqualTo(BattleServiceException.attackingArmyHasAnotherMovement().getMessage());
    }

    @Test
    void ensureCreateBattleThrowsExceptionWhenArmiesNotInSameRegion(){
        log.debug("Testing if createBattle throws exception when armies not in same region!");

        army1.setCurrentRegion(region1);

        var exception = assertThrows(BattleServiceException.class, ()-> battleService.createBattle(createBattleDto));

        assertThat(exception.getMessage()).isEqualTo(BattleServiceException.notInSameRegion(army1, army2).getMessage());
    }

}
