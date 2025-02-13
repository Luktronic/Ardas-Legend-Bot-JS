package com.ardaslegends.service;

import com.ardaslegends.domain.*;
import com.ardaslegends.repository.ArmyRepository;
import com.ardaslegends.repository.claimbuild.ClaimbuildRepository;
import com.ardaslegends.repository.faction.FactionRepository;
import com.ardaslegends.repository.MovementRepository;
import com.ardaslegends.service.dto.army.*;
import com.ardaslegends.service.dto.unit.UnitTypeDto;
import com.ardaslegends.service.exceptions.logic.faction.FactionServiceException;
import com.ardaslegends.service.exceptions.logic.army.ArmyServiceException;
import com.ardaslegends.service.exceptions.logic.claimbuild.ClaimBuildServiceException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ArmyServiceTest {

    private ArmyService armyService;

    private ArmyRepository mockArmyRepository;
    private MovementRepository mockMovementRepository;
    private FactionRepository mockFactionRepository;
    private PlayerService mockPlayerService;
    private UnitTypeService mockUnitTypeService;
    private ClaimbuildRepository mockClaimbuildRepository;

    private BindArmyDto dto;
    private Faction faction;
    private Region region1;
    private Region region2;
    private UnitType unitType;
    private Unit unit;
    private RPChar rpchar;
    private Player player;
    private Army army;
    private Movement movement;
    private ClaimBuild claimBuild;

    @BeforeEach
    void setup() {
        mockArmyRepository = mock(ArmyRepository.class);
        mockMovementRepository = mock(MovementRepository.class);
        mockFactionRepository = mock(FactionRepository.class);
        mockPlayerService = mock(PlayerService.class);
        mockUnitTypeService = mock(UnitTypeService.class);
        mockClaimbuildRepository = mock(ClaimbuildRepository.class);
        armyService = new ArmyService(mockArmyRepository, mockMovementRepository,mockPlayerService, mockFactionRepository, mockUnitTypeService, mockClaimbuildRepository);

        region1 = Region.builder().id("90").build();
        region2 = Region.builder().id("91").build();
        unitType = UnitType.builder().unitName("Gondor Archer").tokenCost(1.5).build();
        unit = Unit.builder().unitType(unitType).army(army).amountAlive(5).count(10).isMounted(false).build();
        faction = Faction.builder().name("Gondor").allies(new ArrayList<>()).build();
        claimBuild = ClaimBuild.builder().name("Nimheria").type(ClaimBuildType.CASTLE).siege("Ram, Trebuchet, Tower").region(region1).ownedBy(faction).specialBuildings(List.of(SpecialBuilding.HOUSE_OF_HEALING)).stationedArmies(List.of()).build();
        rpchar = RPChar.builder().name("Belegorn").isHealing(false).injured(false).currentRegion(region1).build();
        player = Player.builder().discordID("1234").faction(faction).build();
        player.addActiveRpChar(rpchar);
        army = Army.builder().name("Knights of Gondor").armyType(ArmyType.ARMY).faction(faction).units(List.of(unit)).freeTokens(30 - unit.getCount() * unitType.getTokenCost()).currentRegion(region2).stationedAt(claimBuild).sieges(new ArrayList<>()).build();
        movement =  Movement.builder().isCharMovement(false).isCurrentlyActive(true).army(army).path(List.of(PathElement.builder().region(region1).build())).build();

        dto = new BindArmyDto(player.getDiscordID(), player.getDiscordID(), army.getName());

        when(mockPlayerService.getPlayerByDiscordId(player.getDiscordID())).thenReturn(player);
        when(mockArmyRepository.findArmyByName(army.getName())).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);
        when(mockMovementRepository.findMovementByArmyAndIsCurrentlyActiveTrue(army)).thenReturn(Optional.of(movement));
        when(mockClaimbuildRepository.findClaimBuildByName(claimBuild.getName())).thenReturn(Optional.of(claimBuild));
    }

    // Create Army
    @Test
    void ensureCreateArmyWorksProperly() {
        log.debug("Testing if createArmy works properly");

        log.trace("Initializing data");
        CreateArmyDto dto = new CreateArmyDto("Kek", "Kek", ArmyType.ARMY, "Kek",
                new UnitTypeDto[]{new UnitTypeDto("Kek", 11, false),new UnitTypeDto("Kek", 10, false) });
        ClaimBuild claimBuild = new ClaimBuild();
        ClaimBuildType type = ClaimBuildType.TOWN;
        claimBuild.setType(type);
        claimBuild.setFreeArmiesRemaining(1);

        Faction faction = Faction.builder().name("Gondr").build();
        claimBuild.setOwnedBy(faction);
        Player player = Player.builder().discordID(dto.executorDiscordId()).faction(faction).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(player);
        when(mockArmyRepository.findArmyByName(dto.name())).thenReturn(Optional.empty());
        when(mockUnitTypeService.getUnitTypeByName(any())).thenReturn(new UnitType("Kek", 1.0));
        when(mockClaimbuildRepository.findClaimBuildByName(dto.claimBuildName())).thenReturn(Optional.of(claimBuild));
        when(mockArmyRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        log.debug("Calling createArmy()");
        var result = armyService.createArmy(dto);

        assertThat(result.getFreeTokens()).isEqualTo(30-21);
        log.info("Test passed: CreateArmy works properly with correct values");
    }

    @Test
    void ensureCreateArmyThrowsIAEWhenArmyNameIsAlreadyTaken() {
        log.debug("Testing if createArmy correctly throws IAE when name is already taken");

        log.trace("Initializing data");
        CreateArmyDto dto = new CreateArmyDto("Kek", "Kek", ArmyType.ARMY, "Kek", new UnitTypeDto[]{new UnitTypeDto("Kek", 10, false)});

        when(mockArmyRepository.findArmyByName(dto.name())).thenReturn(Optional.of(new Army()));

        log.debug("Expecting IAE on call");
        log.debug("Calling createArmy()");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.createArmy(dto));

        assertThat(result.getMessage()).isEqualTo(ArmyServiceException.armyOrCompanyWithNameAlreadyExists(dto.name()).getMessage());
        log.info("Test passed: IAE when Army Name is taken!");
    }

    @Test
    void ensureCreateArmyThrowsIAEWhenNoClaimBuildWithInputNameHasBeenFound() {
        log.debug("Testing if createArmy correctly throws IAE when no claimBuild could be found");

        log.trace("Initializing data");
        CreateArmyDto dto = new CreateArmyDto("Kek", "Kek", ArmyType.ARMY, "Kek", new UnitTypeDto[]{new UnitTypeDto("Kek", 10, false)});

        Faction faction = Faction.builder().name("Gondr").build();
        Player player = Player.builder().discordID(dto.executorDiscordId()).faction(faction).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(player);
        when(mockArmyRepository.findArmyByName(dto.name())).thenReturn(Optional.empty());
        when(mockUnitTypeService.getUnitTypeByName(any())).thenReturn(new UnitType("Kek", 1.0));
        when(mockClaimbuildRepository.findClaimBuildByName(dto.claimBuildName())).thenReturn(Optional.empty());

        log.debug("Expecting IAE on call");
        log.debug("Calling createArmy()");
        var result = assertThrows(ClaimBuildServiceException.class, () -> armyService.createArmy(dto));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.noCbWithName(dto.claimBuildName()).getMessage());
        log.info("Test passed: IAE when no ClaimBuild could be found");
    }

    @Test
    void ensureCreateArmyThrowsSEWhenCbIsFromDifferentFaction() {
        log.debug("Testing if createArmy correctly throws ArmyServiceException when claimBuild is from another faction");

        log.trace("Initializing data");
        CreateArmyDto dto = new CreateArmyDto(player.getDiscordID(), "Kek2", ArmyType.ARMY, claimBuild.getName(), new UnitTypeDto[]{new UnitTypeDto("Kek", 10, false)});
        Faction otherFaction = Faction.builder().name("Dol Amroth").build();
        claimBuild.setOwnedBy(otherFaction);

        log.debug("Expecting IAE on call");
        log.debug("Calling createArmy()");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.createArmy(dto));

        assertThat(result.getMessage()).isEqualTo(ArmyServiceException.cannotCreateArmyFromClaimbuildInDifferentFaction(player.getFaction().getName(), claimBuild.getOwnedBy().getName(), dto.armyType()).getMessage());
        log.info("Test passed: createArmy throws ArmyServiceException when claimBuild is from another faction");
    }
    @Test
    void ensureCreateArmyThrowsServiceExceptionWhenClaimBuildHasReachedMaxArmies() {
        log.debug("Testing if createArmy correctly throws SE when max armies is already reached");

        log.trace("Initializing data");
        CreateArmyDto dto = new CreateArmyDto("Kek", "Kek", ArmyType.ARMY, "Kek", new UnitTypeDto[]{new UnitTypeDto("Kek", 10, false)});
        ClaimBuild claimBuild = new ClaimBuild();
        ClaimBuildType type = ClaimBuildType.HAMLET;
        claimBuild.setType(type);

        Faction faction = Faction.builder().name("Gondr").build();
        claimBuild.setOwnedBy(faction);
        Player player = Player.builder().discordID(dto.executorDiscordId()).faction(faction).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(player);
        when(mockArmyRepository.findArmyByName(dto.name())).thenReturn(Optional.empty());
        when(mockUnitTypeService.getUnitTypeByName(any())).thenReturn(new UnitType("Kek", 1.0));
        when(mockClaimbuildRepository.findClaimBuildByName(dto.claimBuildName())).thenReturn(Optional.of(claimBuild));

        log.debug("Expecting SE on call");
        log.debug("Calling createArmy()");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.createArmy(dto));

        log.info("Test passed: SE on max armies from ClaimBuild");
    }
    @Test
    void ensureCreateArmyThrowsServiceExceptionWhenUnitsExceedAvailableTokens() {
        log.debug("Testing if createArmy correctly throws SE when units exceed available tokens");

        log.trace("Initializing data");
        CreateArmyDto dto = new CreateArmyDto("Kek", "Kek", ArmyType.ARMY, "Kek", new UnitTypeDto[]{new UnitTypeDto("Kek", 11, false)});
        ClaimBuild claimBuild = new ClaimBuild();
        ClaimBuildType type = ClaimBuildType.TOWN;
        claimBuild.setType(type);

        Faction faction = Faction.builder().name("Gondr").build();
        claimBuild.setOwnedBy(faction);
        Player player = Player.builder().discordID(dto.executorDiscordId()).faction(faction).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(player);
        when(mockArmyRepository.findArmyByName(dto.name())).thenReturn(Optional.empty());
        when(mockUnitTypeService.getUnitTypeByName(any())).thenReturn(new UnitType("Kek", 3.0));
        when(mockClaimbuildRepository.findClaimBuildByName(dto.claimBuildName())).thenReturn(Optional.of(claimBuild));

        log.debug("Expecting SE on call");
        log.debug("Calling createArmy()");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.createArmy(dto));

        log.info("Test passed: SE on exceeding token count");
    }

    // Healing start Tests

    @Test
    void ensureHealStartWorksProperly() {
        log.debug("Testing if heal start works properly with correct values");

        log.trace("Initializing data");

        UpdateArmyDto dto = new UpdateArmyDto(player.getDiscordID(), army.getName(), null, null);

        log.debug("Expecting no errors");
        log.debug("Calling healStart");
        var result = armyService.healStart(dto);

        assertThat(army.getIsHealing()).isTrue();
        log.info("Test passed: heal start works properly with correct values");
    }

    @Test
    void ensureHealStartThrowsSeWhenArmyObjectIsATradingCompany() {
        log.debug("Testing if healStart correctly throws Se when army object is a trading company");

        army.setArmyType(ArmyType.TRADING_COMPANY);

        UpdateArmyDto dto = new UpdateArmyDto(player.getDiscordID(), army.getName(), null, null);

        log.debug("Expecting Se on call, calling healStart");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.healStart(dto));

        assertThat(result.getMessage()).isEqualTo(ArmyServiceException.tradingCompaniesCannotHeal(dto.armyName()).getMessage());
        log.info("Test passed: healStart throws Se when army object is a trading company");
    }
    @Test
    void ensureHealStartThrwosSeWhenArmyAndPlayerAreNotInTheSameFaction() {
        log.debug("Testing if healStart correctly throws SE when Player and Army are not in the same faction");

        log.trace("Initializing data");
        army.setFaction(Faction.builder().name("Kekw").build());

        UpdateArmyDto dto = new UpdateArmyDto(player.getDiscordID(),army.getName(), null, null);

        log.debug("Expecting SE on call");
        log.debug("Calling healStart");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.healStart(dto));

        assertThat(result.getMessage()).contains("are not in the same faction");
        log.info("Test passed: SE if not in the same faction");
    }

    @Test
    void ensureHealStartThrowsSeWhenArmyIsAlreadyFullyHealed() {
        log.debug("Testing if healStart correctly throws SE when army is already fully healed");

        log.trace("Initializing data");
        unit.setAmountAlive(unit.getCount());

        UpdateArmyDto dto = new UpdateArmyDto(player.getDiscordID(), army.getName(),null, null);

        log.debug("Expecting SE on call");
        log.debug("Calling healStart");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.healStart(dto));

        assertThat(result.getMessage()).isEqualTo(ArmyServiceException.alreadyFullyHealed(army.getArmyType(), army.getName()).getMessage());
        log.info("Test passed: SE when army is already fully healed");
    }

    @Test
    void ensureHealStartThrowsSeWhenArmyIsNotStationedAtACbWithHouseOfHealing() {
        log.debug("Testing if healStart correctly throws SE when army is not stationed at House of Healing Cb");

        log.trace("Initializing data");
        claimBuild.setSpecialBuildings(List.of());

        UpdateArmyDto dto = new UpdateArmyDto(player.getDiscordID(), army.getName(),null, null);

        log.debug("Expecting SE on call");
        log.debug("Calling healStart");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.healStart(dto));

        assertThat(result.getMessage()).contains("not stationed at a CB with a House of Healing");
        log.info("Test passed: SE if army is not stationed at House of Healing");
    }

    // Heal stop tests

    @Test
    void ensureHealStopWorksProperly() {
        log.debug("Testing if heal stop works properly with correct values");

        log.trace("Initializing data");
        army.setIsHealing(true);

        UpdateArmyDto dto = new UpdateArmyDto(player.getDiscordID(), army.getName(), null, null);

        log.debug("Expecting no errors");
        log.debug("Calling healStart");
        var result = armyService.healStop(dto);

        assertThat(army.getIsHealing()).isFalse();
        log.info("Test passed: heal stop works properly with correct values");
    }
    @Test
    void ensureHealStopThrowsSeIfArmyIsNotHealing() {
        log.debug("Testing if heal stop correctly throws SE when army is not healing");

        log.trace("Initializing data");
        army.setIsHealing(false);

        UpdateArmyDto dto = new UpdateArmyDto(player.getDiscordID(),army.getName(), null, null);

        log.debug("Expecting SE on call");
        log.debug("Calling healStop");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.healStop(dto));

        assertThat(result.getMessage()).contains("is not healing - Can't stop it");
        log.info("Test passed: SE when army is not healing");
    }

    @Test
    void ensureHealStopThrowsSeIfArmyAndPlayerAreNotInTheSameFaction() {
        log.debug("Testing if heal stop correctly throws SE when army is not in same faction as player");

        log.trace("Initializing data");
        army.setIsHealing(true);
        army.setFaction(Faction.builder().name("Kekw").build());

        UpdateArmyDto dto = new UpdateArmyDto(player.getDiscordID(),army.getName(), null, null);

        log.debug("Expecting SE on call");
        log.debug("Calling healStop");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.healStop(dto));

        assertThat(result.getMessage()).contains("are not in the same faction");
        log.info("Test passed: SE when army is not in same faction as player");
    }

    // Station Tests

    @Test
    void ensureStationWorksProperlyWhenPlayerIsFactionLeader() {
        log.debug("Testing if station works properly when player is faction leader of army");

        army.setStationedAt(null);
        army.getFaction().setLeader(player);

        StationDto dto = new StationDto(player.getDiscordID(), army.getName(), claimBuild.getName());

        log.debug("Calling station(), expecting no errors");
        Army result = armyService.station(dto);

        assertThat(army.getStationedAt()).isEqualTo(claimBuild);
    }

    @Test
    void ensureStationThrowsCbSeWhenClaimbuildWithGivenNameDoesNotExist() {
        log.debug("Testing if station throws CB Se when no claimbuild exists with given name");

        log.trace("Initializing data");
        when(mockClaimbuildRepository.findClaimBuildByName(claimBuild.getName())).thenReturn(Optional.empty());

        StationDto dto = new StationDto(player.getDiscordID(),army.getName(),claimBuild.getName());

        log.debug("Expecting SE on call");
        log.debug("Calling station()");
        var result = assertThrows(ClaimBuildServiceException.class, () -> armyService.station(dto));

        assertThat(result.getMessage()).contains("Found no claimbuild with name");
        log.info("Test passed: station throws SE when no cb found");
    }

    @Test
    void ensureStationThrowsSeWhenArmyIsAlreadyStationed() {
        log.debug("Testing if station throws Se when army is already stationed");

        StationDto dto = new StationDto(player.getDiscordID(),army.getName(),claimBuild.getName());

        log.debug("Calling station(), expecting ArmySe");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.station(dto));

        assertThat(result.getMessage()).contains("is already stationed at Claimbuild");
        log.info("Test passed: station throws Se when already stationed");
    }

    @Test
    void ensureStationThrowsSeWhenClaimbuildIsNotInTheSameOrAlliedFaction() {
        log.debug("Testing if station throws Se when claimbuild is not in the same or allied faction");

        claimBuild.setOwnedBy(Faction.builder().name("Kek123").build());
        army.setStationedAt(null);
        StationDto dto = new StationDto(player.getDiscordID(),army.getName(),claimBuild.getName());

        log.debug("Calling station(), expecting Se");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.station(dto));

        assertThat(result.getMessage()).contains("is not in the same or allied faction");
        log.info("Test passed: station throws Se when claimbuild is not in the same or allied faction");


    }

    @Test
    void ensureStationThrowsSeWhenPlayerHasNoPermissionToPerformAction() {
        log.debug("Testing if station throws Se when player is not allowed to perform action");

        army.setStationedAt(null);
        army.setBoundTo(null);

        StationDto dto = new StationDto(player.getDiscordID(), army.getName(), claimBuild.getName());

        log.debug("Calling station(), expecting Se");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.station(dto));

        assertThat(result.getMessage()).contains("No permission to perform this action.");
        log.info("Test passed: station throws Se when player has no permission to perform station");
    }

    // Unstation tests

    @Test
    void ensureUnstationWorksProperly() {
        log.debug("Testing if unstation works properly with correct values");

        army.setBoundTo(player.getActiveCharacter().get());

        UnstationDto dto = new UnstationDto(player.getDiscordID(), army.getName());

        log.debug("Calling unstation, expecting no errors");
        var result = armyService.unstation(dto);

        assertThat(result.getStationedAt()).isNull();
        log.info("Test passed: Unstation works correctly with correct values");
    }
    @Test
    void ensureUnstationThrowsSeWhenArmyIsNotStationed() {
        log.debug("Testing if unstation() throws Se when army is not stationed at a Cb");

        army.setStationedAt(null);

        UnstationDto dto = new UnstationDto(player.getDiscordID(), army.getName());

        log.debug("calling unstation(), expecting Se");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.unstation(dto));

        assertThat(result.getMessage()).contains("is not stationed at a Claimbuild");
        log.info("Test passed: unstation throws Se when army is not stationed at a Claimbuild");
    }

    @Test
    void ensureUnstationThrowsSeWhenPlayerNotAllowedToPerformAction() {
        log.debug("Testing if unstation() throws Se when Player does not have the permission to perform the action");

        army.setBoundTo(null);

        UnstationDto dto = new UnstationDto(player.getDiscordID(), army.getName());

        log.debug("Calling unstation(), expecting Se");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.unstation(dto));

        assertThat(result.getMessage()).contains("No permission to perform this action.");
    }

    @Test
    void ensureBindWorksWhenBindingSelf() {
        log.debug("Testing if army binding works properly!");

        //Assign
        log.trace("Initializing data");
        Faction faction = Faction.builder().name("Gondor").build();
        Region region = Region.builder().id("90").build();
        RPChar rpChar = RPChar.builder().name("Belegorn").injured(false).isHealing(false).currentRegion(region).build();
        Player player = Player.builder().ign("Lüktrönic").discordID("1").faction(faction).build();
        player.addActiveRpChar(rpChar);
        rpChar.setOwner(player);
        Army army = Army.builder().name("Gondorian Army").currentRegion(region).armyType(ArmyType.ARMY).faction(faction).build();

        BindArmyDto dto = new BindArmyDto("1", "1", "Gondorian Army");

        when(mockPlayerService.getPlayerByDiscordId("1")).thenReturn(player);
        when(mockArmyRepository.findArmyByName("Gondorian Army")).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);

        log.debug("Calling bind()");
        armyService.bind(dto);

        assertThat(army.getBoundTo().getOwner()).isEqualTo(player);
        log.info("Test passed: army binding works properly!");
    }

    @Test
    void ensureBindWorksWhenBindingOtherPlayer() {
        log.debug("Testing if army binding works properly on others!");

        //Assign
        log.trace("Initializing data");
        Faction faction = Faction.builder().name("Gondor").build();
        Region region = Region.builder().id("90").build();
        RPChar rpChar = RPChar.builder().name("Belegorn").injured(false).isHealing(false).currentRegion(region).build();
        Player executor = Player.builder().ign("Lüktrönic").discordID("1").faction(faction).build();
        executor.addActiveRpChar(rpChar);
        Player target = Player.builder().ign("aned").discordID("2").faction(faction).build();
        target.addActiveRpChar(rpChar);
        Army army = Army.builder().name("Gondorian Army").currentRegion(region).armyType(ArmyType.ARMY).faction(faction).build();

        faction.setLeader(executor);

        BindArmyDto dto = new BindArmyDto("1", "2", "Gondorian Army");

        when(mockPlayerService.getPlayerByDiscordId("1")).thenReturn(executor);
        when(mockPlayerService.getPlayerByDiscordId(dto.targetDiscordId())).thenReturn(target);
        when(mockArmyRepository.findArmyByName("Gondorian Army")).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);

        log.debug("Calling bind()");
        armyService.bind(dto);

        assertThat(army.getBoundTo()).isEqualTo(target.getActiveCharacter().get());
        log.info("Test passed: army binding works properly on other players as faction leader!");
    }
    @Test
    void ensureBindArmyThrowsServiceExceptionWhenNormalPlayerTriesToBindOtherPlayers() {
        log.debug("Testing if SE is thrown when normal player tries to bind other players");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Anedhel", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).build();
        Player aned = Player.builder().discordID(dto.targetDiscordId()).faction(gondor).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(luk);

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));
    }
    @Test
    void ensureBindArmyThrowsServiceExceptionWhenTargetArmyHasDifferentFaction() {
        log.debug("Testing if SE is thrown when target army has a different faction to the target player");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Faction mordor = Faction.builder().name("Mordor").build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).rpChars(new HashSet<>(Set.of(RPChar.builder().active(true).build()))).build();
        Player aned = Player.builder().discordID(dto.targetDiscordId()).faction(gondor).rpChars(new HashSet<>(Set.of(RPChar.builder().active(true).build()))).build();
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(mordor).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(luk);
        when(mockArmyRepository.findArmyByName(dto.armyName())).thenReturn(Optional.of(army));

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));

        log.info("Test passed: bind() correctly throws SE when Army is from a different faction");
    }
    @Test
    void ensureBindArmyThrowsServiceExceptionWhenTargetPlayerIsWandererAndExecutorIsNotFactionLeaderOrLord() {
        log.debug("Testing if SE is thrown when target player is wanderer and executor is not leader or lord");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Faction wanderer = Faction.builder().name("Wanderer").build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).rpChars(new HashSet<>(Set.of(RPChar.builder().active(true).build()))).build();
        Player mirak = Player.builder().discordID(dto.targetDiscordId()).faction(wanderer).rpChars(new HashSet<>(Set.of(RPChar.builder().active(true).build()))).build();
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(luk);
        when(mockPlayerService.getPlayerByDiscordId(dto.targetDiscordId())).thenReturn(mirak);
        when(mockArmyRepository.findArmyByName(dto.armyName())).thenReturn(Optional.of(army));

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));

        log.info("Test passed: bind() correctly throws SE when target player is wanderer and executor is not leader or lord");
    }
    @Test
    void ensureBindArmyThrowsServiceExceptionWhenTargetArmyIsInADifferentRegion() {
        log.debug("Testing if SE is thrown when target army is in a different region to the player");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Region region1 = Region.builder().id("90").build();
        Region region2 = Region.builder().id("91").build();
        RPChar rpchar = RPChar.builder().name("Belegorn").currentRegion(region1).build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).build();
        luk.addActiveRpChar(rpchar);
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).currentRegion(region2).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(luk);
        when(mockArmyRepository.findArmyByName(dto.armyName())).thenReturn(Optional.of(army));

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));

        log.info("Test passed: bind() correctly throws SE when Army is in a different region");
    }

    @Test
    void ensureBindArmyThrowsServiceExceptionWhenTargetArmyIsBoundToAPlayer() {
        log.debug("Testing if SE is thrown when target army is already bound to a player");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Region region = Region.builder().id("90").build();
        RPChar rpchar = RPChar.builder().name("Belegorn").currentRegion(region).build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).build();
        luk.addActiveRpChar(rpchar);
        rpchar.setOwner(luk);
        Player aned = Player.builder().discordID("1235").faction(gondor).build();
        aned.addActiveRpChar(rpchar);
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).currentRegion(region).boundTo(aned.getActiveCharacter().get()).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(luk);
        when(mockArmyRepository.findArmyByName(dto.armyName())).thenReturn(Optional.of(army));

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));

        log.info("Test passed: bind() correctly throws SE when Army is already bound to another player");
    }

    @Test
    void ensureBindArmyThrowsServiceExceptionWhenRpCharIsInjured() {
        log.debug("Testing if SE is thrown SE when rpchar is injured");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Region region = Region.builder().id("90").build();
        RPChar rpchar = RPChar.builder().name("Belegorn").injured(true).currentRegion(region).build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).build();
        luk.addActiveRpChar(rpchar);
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).currentRegion(region).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(luk);
        when(mockArmyRepository.findArmyByName(dto.armyName())).thenReturn(Optional.of(army));

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));

        assertThat(result.getMessage()).isEqualTo(ArmyServiceException.cannotBindCharInjured(rpchar.getName(), army.getName()).getMessage());
        log.info("Test passed: bind() correctly throws SE when rpchar is injured");
    }

    @Test
    void ensureBindArmyThrowsServiceExceptionWhenRpCharIsHealing() {
        log.debug("Testing if SE is thrown SE when rpchar is healing");

        log.trace("Initializing data");

        rpchar.setIsHealing(true);

        army.setCurrentRegion(rpchar.getCurrentRegion());

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(player);
        when(mockArmyRepository.findArmyByName(dto.armyName())).thenReturn(Optional.of(army));

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));

        assertThat(result.getMessage()).isEqualTo(ArmyServiceException.cannotBindCharHealing(rpchar.getName(), army.getName()).getMessage());
        log.info("Test passed: bind() correctly throws SE when rpchar is healing");
    }

    @Test
    void ensureBindArmyThrowsServiceExceptionWhenArmyIsMoving() {
        log.debug("Testing if SE is thrown when army is currently moving!");

        log.trace("Initializing data");

        army.setCurrentRegion(rpchar.getCurrentRegion());
        Movement move = Movement.builder().isCharMovement(false).isCurrentlyActive(true).army(army).path(List.of(PathElement.builder().region(region1).build())).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(player);
        when(mockArmyRepository.findArmyByName(dto.armyName())).thenReturn(Optional.of(army));
        when(mockMovementRepository.findMovementByArmyAndIsCurrentlyActiveTrue(army)).thenReturn(Optional.of(move));

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));

        assertThat(result.getMessage()).isEqualTo(ArmyServiceException.cannotBindArmyIsMoving(army.getArmyType(), army.getName(), move.getDestinationRegionId()).getMessage());
        log.info("Test passed: bind() correctly throws SE when army is currently moving!");
    }

    @Test
    void ensureBindArmyThrowsServiceExceptionWhenCharIsMoving() {
        log.debug("Testing if SE is thrown when character is currently moving!");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Region region = Region.builder().id("90").build();
        RPChar rpchar = RPChar.builder().injured(false).isHealing(false).name("Belegorn").currentRegion(region).build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).build();
        luk.addActiveRpChar(rpchar);
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).currentRegion(region).boundTo(null).build();
        Movement move = Movement.builder().isCharMovement(false).isCurrentlyActive(true).rpChar(luk.getActiveCharacter().get()).path(List.of(PathElement.builder().region(region1).build())).build();

        when(mockPlayerService.getPlayerByDiscordId(dto.executorDiscordId())).thenReturn(luk);
        when(mockArmyRepository.findArmyByName(dto.armyName())).thenReturn(Optional.of(army));
        when(mockMovementRepository.findMovementByRpCharAndIsCurrentlyActiveTrue(luk.getActiveCharacter().get())).thenReturn(Optional.of(move));

        log.debug("Calling bind()");
        log.trace("Expecting ServiceException");
        var result = assertThrows(ArmyServiceException.class, () -> armyService.bind(dto));

        log.info("Test passed: bind() correctly throws SE when character is currently moving!");
    }

    @Test
    void ensureUnbindWorks() {
        log.debug("Testing if unbinding players from armies works");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        RPChar rpchar = RPChar.builder().name("Belegorn").build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).build();
        luk.addActiveRpChar(rpchar);
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).boundTo(luk.getActiveCharacter().get()).build();


        when(mockArmyRepository.findArmyByName(army.getName())).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);
        when(mockPlayerService.getPlayerByDiscordId(luk.getDiscordID())).thenReturn(luk);

        log.debug("Calling unbind()");
        armyService.unbind(dto);

        assertThat(army.getBoundTo()).isNull();
        log.info("Test passed: unbind() works with proper data!");
    }

    @Test
    void ensureUnbindThrowsSEWhenTargetIsOtherPlayer() {
        log.debug("Testing if unbinding throws ServiceException when target is other player and executor is not leader!");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "mirak", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        RPChar rpchar = RPChar.builder().name("Tinwe").build();
        Player luk = Player.builder().discordID(dto.executorDiscordId()).faction(gondor).build();
        Player mirak = Player.builder().discordID(dto.targetDiscordId()).faction(gondor).build();
        mirak.addActiveRpChar(rpchar);
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).boundTo(mirak.getActiveCharacter().get()).build();

        when(mockArmyRepository.findArmyByName(army.getName())).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);
        when(mockPlayerService.getPlayerByDiscordId(luk.getDiscordID())).thenReturn(luk);
        when(mockPlayerService.getPlayerByDiscordId(mirak.getDiscordID())).thenReturn(mirak);

        log.debug("Calling unbind()");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.unbind(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.notFactionLeader(gondor.getName()).getMessage());
        log.info("Test passed: unbind() throws ArmyServiceException when target is other player and executor is not leader");
    }

    @Test
    void ensureUnbindWorksWhenTargetIsOtherPlayerAndExecutorIsLeader() {
        log.debug("Testing if unbinding works when target is other player and executor is leader!");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "mirak", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        RPChar rpchar = RPChar.builder().name("Tinwe").build();
        Player luk = Player.builder().ign("Luk").discordID(dto.executorDiscordId()).faction(gondor).build();
        Player mirak = Player.builder().ign("mirak").discordID(dto.targetDiscordId()).faction(gondor).build();
        mirak.addActiveRpChar(rpchar);
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).boundTo(mirak.getActiveCharacter().get()).build();
        gondor.setLeader(luk);

        when(mockArmyRepository.findArmyByName(army.getName())).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);
        when(mockPlayerService.getPlayerByDiscordId(luk.getDiscordID())).thenReturn(luk);
        when(mockPlayerService.getPlayerByDiscordId(mirak.getDiscordID())).thenReturn(mirak);

        log.debug("Calling unbind()");
        armyService.unbind(dto);

        assertThat(army.getBoundTo()).isNull();
        log.info("Test passed: unbind() works when target is other player and executor is leader");
    }

    @Test
    void ensureUnbindThrowsSEWhenNoPlayerBound() {
        log.debug("Testing if unbinding throws ArmyServiceException when no player is bound to the army!");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Player luk = Player.builder().ign("Luk").discordID(dto.executorDiscordId()).faction(gondor).build();
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).boundTo(null).build();

        when(mockArmyRepository.findArmyByName(army.getName())).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);
        when(mockPlayerService.getPlayerByDiscordId(luk.getDiscordID())).thenReturn(luk);

        log.debug("Calling unbind()");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.unbind(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.noPlayerBoundToArmy(army.getArmyType(), army.getName()).getMessage());
        log.info("Test passed: unbind() throws ArmyServiceException when no player is bound to the army!");
    }

    @Test
    void ensureUnbindThrowsSEWhenArmyMoving() {
        log.debug("Testing if unbinding throws ArmyServiceException when the army is currently moving!");

        log.trace("Initializing data");
        BindArmyDto dto = new BindArmyDto("Luktronic", "Luktronic", "Slayers of Orcs");
        Faction gondor = Faction.builder().name("Gondor").build();
        Player luk = Player.builder().ign("Luk").discordID(dto.executorDiscordId()).faction(gondor).build();
        Army army = Army.builder().name(dto.armyName()).armyType(ArmyType.ARMY).faction(gondor).boundTo(null).build();
        gondor.setLeader(luk);
        PathElement pathElement1 = PathElement.builder().region(Region.builder().id("90").build()).build();
        PathElement pathElement2 = PathElement.builder().region(Region.builder().id("91").build()).build();
        Movement movement = Movement.builder().army(army).path(List.of(pathElement1, pathElement2)).isCurrentlyActive(true).build();

        when(mockArmyRepository.findArmyByName(army.getName())).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);
        when(mockPlayerService.getPlayerByDiscordId(luk.getDiscordID())).thenReturn(luk);
        when(mockMovementRepository.findMovementByArmyAndIsCurrentlyActiveTrue(army)).thenReturn(Optional.of(movement));

        log.debug("Calling unbind()");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.unbind(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.noPlayerBoundToArmy(army.getArmyType(), army.getName()).getMessage());
        log.info("Test passed: unbind() throws ArmyServiceException when no player is bound to the army!");
    }

    @Test
    void ensureGetArmyByNameThrowsServiceExceptionWhenArmyNotFound() {
        log.debug("Testing if ASE is thrown when target army does not exist");

        log.trace("Initializing data");
        String armyName = "Knights of Gondor";

        when(mockArmyRepository.findArmyByName(armyName)).thenReturn(Optional.empty());

        log.debug("Calling getArmyByName");
        log.trace("Expecting ArmyServiceException");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.getArmyByName(armyName));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.noArmyWithName("Army or Company", armyName).getMessage());
        log.info("Test passed: getArmyByName() correctly throws ASE when no Army has been found");
    }

    @Test
    void ensureDisbandArmyWorks() {
        log.debug("Testing if disbandArmy works with proper data!");

        faction.setLeader(player);

        log.trace("Initializing data");
        DeleteArmyDto dto = new DeleteArmyDto(player.getDiscordID(), army.getName());

        log.debug("Calling disbandArmy");
        Army returnedArmy = armyService.disbandFromDto(dto, false);

        log.debug("Asserting that returned/deleted army is same as inputted army");
        assertThat(returnedArmy).isEqualTo(army);
        log.info("Test passed: disbandArmy works with proper data!");

    }

    @Test
    void ensureDisbandArmyThrowsSEWhenPlayerIsOtherFaction() {
        log.debug("Testing if disbandArmy throws ArmyServiceException when player is in other faction than army");

        log.trace("Initializing data");
        Faction otherFaction = Faction.builder().name("Dol Amroth").build();
        player.setFaction(otherFaction);

        DeleteArmyDto dto = new DeleteArmyDto(player.getDiscordID(), army.getName());

        log.debug("Calling disbandArmy");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.disbandFromDto(dto, false));

        log.debug("Asserting that error has correct message");
        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.notAllowedToDisbandNotSameFaction(army.getArmyType(), army.getName(), army.getFaction().getName()).getMessage());
        log.info("Test passed: disbandArmy throws ArmyServiceException when player is in other faction than army");
    }

    @Test
    void ensureDisbandArmyThrowsSEWhenPlayerIsNotLeader() {
        log.debug("Testing if disbandArmy throws ArmyServiceException when player is not faction leader");

        log.trace("Initializing data");
        DeleteArmyDto dto = new DeleteArmyDto(player.getDiscordID(), army.getName());

        log.debug("Calling disbandArmy");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.disbandFromDto(dto, false));

        log.debug("Asserting that error has correct message");
        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.notAllowedToDisband(army.getArmyType()).getMessage());
        log.info("Test passed: disbandArmy throws ArmyServiceException when player is not faction leader");
    }

    @Test
    void ensureDisbandSetsBoundToToNull() {
        log.debug("Testing if disbandArmy sets boundTo to null!");

        faction.setLeader(player);
        army.setBoundTo(player.getActiveCharacter().get());
        rpchar.setBoundTo(army);

        log.trace("Initializing data");
        DeleteArmyDto dto = new DeleteArmyDto(player.getDiscordID(), army.getName());

        log.debug("Calling disbandArmy");
        Army returnedArmy = armyService.disbandFromDto(dto, false);

        assertThat(returnedArmy.getBoundTo()).isNull();
        assertThat(rpchar.getBoundTo()).isNull();
        log.info("Test passed: disbandArmy sets boundTo to null!");

    }

    @Test
    void ensureForcedDisbandArmyWorks() {
        log.debug("Testing if disbandArmy works when forced!");

        log.trace("Initializing data");
        Faction otherFaction = Faction.builder().name("Dol Amroth").build();
        player.setFaction(otherFaction);

        DeleteArmyDto dto = new DeleteArmyDto(player.getDiscordID(), army.getName());

        log.debug("Calling disbandArmy");
        Army returnedArmy = armyService.disbandFromDto(dto, true);

        log.debug("Asserting that returned/deleted army is same as inputted army");
        assertThat(returnedArmy).isEqualTo(army);
        log.info("Test passed: disbandArmy works when forced!");
    }

    @Test
    void ensureSetArmyTokensWorks() {
        log.debug("Testing if setArmyTokens works with proper data!");

        log.trace("Initializing data");
        UpdateArmyDto dto = new UpdateArmyDto(null, army.getName(), 10.0, null);

        log.debug("Calling setArmyTokens");
        Army returnedArmy = armyService.setFreeArmyTokens(dto);

        assertThat(army.getFreeTokens()).isEqualTo(dto.freeTokens());
        assertThat(returnedArmy).isEqualTo(army);
        log.info("Test passed: setArmyTokens works with proper data!");
    }

    @Test
    void ensureSetArmyTokensThrowsSEWhenTokenAbove30() {
        log.debug("Testing if setArmyTokens throws ArmyServiceException when trying to set tokens to value above 30!");

        log.trace("Initializing data");
        UpdateArmyDto dto = new UpdateArmyDto(null, army.getName(), 40.0, null);

        log.debug("Calling setArmyTokens");
        var exception = assertThrows(ArmyServiceException.class ,() -> armyService.setFreeArmyTokens(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.tokenAbove30(dto.freeTokens()).getMessage());
        log.info("Test passed: setArmyTokens throws ArmyServiceException when trying to set tokens to value above 30!");
    }

    @Test
    void ensureSetArmyTokensThrowsSEWhenTokenNegative() {
        log.debug("Testing if setArmyTokens throws ArmyServiceException when trying to set tokens to negative value!");

        log.trace("Initializing data");
        UpdateArmyDto dto = new UpdateArmyDto(null, army.getName(), -1.0, null);

        log.debug("Calling setArmyTokens");
        var exception = assertThrows(ArmyServiceException.class ,() -> armyService.setFreeArmyTokens(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.tokenNegative(dto.freeTokens()).getMessage());
        log.info("Test passed: setArmyTokens throws ArmyServiceException when trying to set tokens to negative value!");
    }


    @Test
    void ensurePickSiegeWorks() {
        log.debug("Testing if pickSiege works with proper data!");

        log.trace("Initializing data");
        String siege = "trebuchet";
        PickSiegeDto dto = new PickSiegeDto(player.getDiscordID(),army.getName(), claimBuild.getName(), siege);
        army.setBoundTo(player.getActiveCharacter().get());
        army.setCurrentRegion(claimBuild.getRegion());

        log.debug("Calling pickSiege");
        Army returnedArmy = armyService.pickSiege(dto);

        assertThat(returnedArmy).isEqualTo(army);
        assertThat(army.getSieges()).contains(siege);
        log.info("Test passed: pickSiege works with proper data!");
    }

    @Test
    void ensurePickSiegeWorksWhenPlayerIsLeader() {
        log.debug("Testing if pickSiege works when player is leader!");

        log.trace("Initializing data");
        String siege = "trebuchet";
        PickSiegeDto dto = new PickSiegeDto(player.getDiscordID(),army.getName(), claimBuild.getName(), siege);
        faction.setLeader(player);
        army.setCurrentRegion(claimBuild.getRegion());

        log.debug("Calling pickSiege");
        Army returnedArmy = armyService.pickSiege(dto);

        assertThat(returnedArmy).isEqualTo(army);
        assertThat(army.getSieges()).contains(siege);
        log.info("Test passed: pickSiege works when player is leader!");
    }

    @Test
    void ensurePickSiegeThrowsSEWhenArmyIsCompany() {
        log.debug("Testing if pickSiege throws ArmyServiceException when army is Trading Company!");

        log.trace("Initializing data");
        String siege = "trebuchet";
        army.setArmyType(ArmyType.TRADING_COMPANY);
        PickSiegeDto dto = new PickSiegeDto(player.getDiscordID(),army.getName(), claimBuild.getName(), siege);

        log.debug("Calling pickSiege");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.pickSiege(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.siegeOnlyArmyCanPick(army.getName()).getMessage());
        log.info("Test passed: pickSiege throws ArmyServiceException when army is Trading Company!");
    }

    @Test
    void ensurePickSiegeThrowsSEWhenPlayerIsNotAllowed() {
        log.debug("Testing if pickSiege throws ArmyServiceException when player is not allowed!");

        log.trace("Initializing data");
        String siege = "trebuchet";
        PickSiegeDto dto = new PickSiegeDto(player.getDiscordID(),army.getName(), claimBuild.getName(), siege);
        army.setCurrentRegion(claimBuild.getRegion());

        log.debug("Calling pickSiege");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.pickSiege(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.siegeNotFactionLeaderOrLord(faction.getName(), army.getName()).getMessage());
        log.info("Test passed: pickSiege throws ArmyServiceException when player is not allowed!");
    }

    @Test
    void ensurePickSiegeThrowsSEWhenCbNotFound() {
        log.debug("Testing if pickSiege throws ClaimBuildServiceException when no CB with name is found!");

        log.trace("Initializing data");
        String siege = "trebuchet";
        PickSiegeDto dto = new PickSiegeDto(player.getDiscordID(),army.getName(), claimBuild.getName(), siege);
        faction.setLeader(player);
        army.setCurrentRegion(claimBuild.getRegion());

        when(mockClaimbuildRepository.findClaimBuildByName(claimBuild.getName())).thenReturn(Optional.empty());

        log.debug("Calling pickSiege");
        var exception = assertThrows(ClaimBuildServiceException.class, () -> armyService.pickSiege(dto));

        assertThat(exception.getMessage()).isEqualTo(ClaimBuildServiceException.noCbWithName(claimBuild.getName()).getMessage());
        log.info("Test passed: pickSiege throws ClaimBuildServiceException when no CB with name is found!");
    }

    @Test
    void ensurePickSiegeThrowsSEWhenArmyNotInSameRegionAsCb() {
        log.debug("Testing if pickSiege throws ArmyServiceException when army is in different region than cb!");

        log.trace("Initializing data");
        String siege = "trebuchet";
        PickSiegeDto dto = new PickSiegeDto(player.getDiscordID(),army.getName(), claimBuild.getName(), siege);
        faction.setLeader(player);

        log.debug("Calling pickSiege");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.pickSiege(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.siegeArmyNotInSameRegionAsCB(army.getName(), army.getCurrentRegion().getId(),
                claimBuild.getName(), claimBuild.getRegion().getId()).getMessage());
        log.info("Test passed: pickSiege throws ArmyServiceException when army is in different region than cb!");
    }

    @Test
    void ensurePickSiegeThrowsSEWhenCbNotSameFactionOrAllied() {
        log.debug("Testing if pickSiege throws ClaimBuildServiceException when CB isn't from same faction or allied faction!");

        log.trace("Initializing data");
        String siege = "trebuchet";
        PickSiegeDto dto = new PickSiegeDto(player.getDiscordID(),army.getName(), claimBuild.getName(), siege);
        Faction faction2 = Faction.builder().name("Dol Amroth").build();
        faction.setLeader(player);
        army.setCurrentRegion(claimBuild.getRegion());
        claimBuild.setOwnedBy(faction2);

        log.debug("Calling pickSiege");
        var exception = assertThrows(ClaimBuildServiceException.class, () -> armyService.pickSiege(dto));

        assertThat(exception.getMessage()).isEqualTo(ClaimBuildServiceException.differentFactionNotAllied(claimBuild.getName(), claimBuild.getOwnedBy().getName()).getMessage());
        log.info("Test passed: pickSiege throws ClaimBuildServiceException when CB isn't from same faction or allied faction!");
    }

    @Test
    void ensurePickSiegeThrowsSEWhenSiegeNotAvailableInCb() {
        log.debug("Testing if pickSiege throws ArmyServiceException when siege is not available in cb!");

        log.trace("Initializing data");
        String siege = "hueueheuheu";
        PickSiegeDto dto = new PickSiegeDto(player.getDiscordID(),army.getName(), claimBuild.getName(), siege);
        faction.setLeader(player);
        army.setCurrentRegion(claimBuild.getRegion());

        log.debug("Calling pickSiege");
        var exception = assertThrows(ArmyServiceException.class, () -> armyService.pickSiege(dto));

        assertThat(exception.getMessage()).isEqualTo(ArmyServiceException.siegeNotAvailable(siege, claimBuild.getName(), claimBuild.getSiege()).getMessage());
        log.info("Test passed: pickSiege throws ArmyServiceException when siege is not available in cb!");
    }

    @Test
    void ensureUpkeepWorksCorrectly() {
        log.debug("Testing if upkeep works correctly");

        Army army1 = Army.builder().armyType(ArmyType.ARMY).build();
        Army army2 = Army.builder().armyType(ArmyType.ARMY).build();
        Army army3 = Army.builder().armyType(ArmyType.ARMY).build();
        Army army4= Army.builder().armyType(ArmyType.ARMY).build();


        Army army5= Army.builder().armyType(ArmyType.TRADING_COMPANY).build();
        Army army6 = Army.builder().armyType(ArmyType.TRADING_COMPANY).build();

        Faction faction1 = Faction.builder().name("Gondor")
                .armies(List.of(army1, army2, army3, army4, army5, army6)).build();

        Faction faction2 = Faction.builder().name("Mordor")
                .armies(List.of(army3, army1, army6, army5)).build();

        when(mockFactionRepository.findAll()).thenReturn(List.of(faction1, faction2));

        List<UpkeepDto> dto = armyService.upkeep();

        assertThat(dto).isNotNull();
        assertThat(dto.size()).isEqualTo(2);
        assertThat(dto.stream().filter(upkeepDto -> upkeepDto.faction().equals("Gondor")).findFirst().isPresent()).isTrue();
        assertThat(dto.stream().filter(upkeepDto -> upkeepDto.faction().equals("Gondor")).findFirst().get().upkeep()).isEqualTo(4000);
        assertThat(dto.stream().filter(upkeepDto -> upkeepDto.faction().equals("Gondor")).findFirst().get().numberOfArmies()).isEqualTo(4);

        assertThat(dto.stream().filter(upkeepDto -> upkeepDto.faction().equals("Mordor")).findFirst().isPresent()).isTrue();
        assertThat(dto.stream().filter(upkeepDto -> upkeepDto.faction().equals("Mordor")).findFirst().get().upkeep()).isEqualTo(2000);
        assertThat(dto.stream().filter(upkeepDto -> upkeepDto.faction().equals("Mordor")).findFirst().get().numberOfArmies()).isEqualTo(2);

    }

    @Test
    void ensureUpkeepPerFactionWorksProperly() {
        log.debug("Testing if upkeepPerFaction works properly with correct values");

        String factionName = "Great Kingdom of Morrivendell";

        Army army1 = Army.builder().armyType(ArmyType.ARMY).build();
        Army army2 = Army.builder().armyType(ArmyType.ARMY).build();
        Army army3 = Army.builder().armyType(ArmyType.ARMY).build();
        Army army4= Army.builder().armyType(ArmyType.ARMY).build();


        Army army5= Army.builder().armyType(ArmyType.TRADING_COMPANY).build();
        Army army6 = Army.builder().armyType(ArmyType.TRADING_COMPANY).build();

        Faction faction1 = Faction.builder().name(factionName)
                .armies(List.of(army1, army2, army3, army4, army5, army6)).build();

        when(mockFactionRepository.findFactionByName(factionName)).thenReturn(Optional.of(faction1));

        log.debug("Calling armyService.upkeepPerFaction, expecting no errors");
        var result = armyService.getUpkeepOfFaction(factionName);

        assertThat(result).isNotNull();
        assertThat(result.faction()).isEqualTo(factionName);
        assertThat(result.numberOfArmies()).isEqualTo(4);
        assertThat(result.upkeep()).isEqualTo(4000);

    }

    @Test
    void ensureUpkeepPerFactionThrowsSeWhenProvidedFactionNameDoesNotHaveAFactionInDatabase() {
        log.debug("Testing if upkeepPerFaction rightly throws a FactionSe when no faction with provided faction name was found in database");

        String factionName = "I dont exist";

        when(mockFactionRepository.findFactionByName(factionName)).thenReturn(Optional.empty());

        log.debug("Calling armyService.upkeepPerFaction(), expecting Se");
        var result = assertThrows(FactionServiceException.class, () -> armyService.getUpkeepOfFaction(factionName));

        assertThat(result.getMessage()).isEqualTo(FactionServiceException.NO_FACTION_WITH_NAME_FOUND_AND_ALL.formatted(factionName, factionName));
        log.info("UpkeepPerFaction correctly throws Se, with correct message, when provided factionName does not exist in database");
    }

    @Test
    void ensureSetIsPaidCorrectlyWorks() {
        log.debug("Testing if setIsPaid correctly works with good values");

        Army army = Army.builder().name("Kek").armyType(ArmyType.ARMY).isPaid(false).build();

        UpdateArmyDto dto = new UpdateArmyDto(null, army.getName(), null, true);
        when(mockArmyRepository.findArmyByName(army.getName())).thenReturn(Optional.of(army));
        when(mockArmyRepository.save(army)).thenReturn(army);

        log.debug("Calling armyService.setIsPaid, expecting no errors");
        var result = armyService.setIsPaid(dto);

        assertThat(result.getIsPaid()).isEqualTo(dto.isPaid());
    }

    @Test
    void ensureGetUnpaidArmiesWorksProperly() {
        log.debug("Testing if getUnpaidArmies works properly");

        Army army1 = Army.builder().name("2000").createdAt(OffsetDateTime.of(2000, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army2 = Army.builder().name("2001").createdAt(OffsetDateTime.of(2001, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army3 = Army.builder().name("2002").createdAt(OffsetDateTime.of(2002, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army4 = Army.builder().name("2003").createdAt(OffsetDateTime.of(2003, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army5 = Army.builder().name("2004").createdAt(OffsetDateTime.of(2004, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army6 = Army.builder().name("2005").createdAt(OffsetDateTime.of(2005, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army7 = Army.builder().name("2006").createdAt(OffsetDateTime.of(2006, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army8 = Army.builder().name("2007").createdAt(OffsetDateTime.of(2007, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army9 = Army.builder().name("2008").createdAt(OffsetDateTime.of(2008, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army10 = Army.builder().name("2009").createdAt(OffsetDateTime.of(2009, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army11 = Army.builder().name("2010").createdAt(OffsetDateTime.of(2010, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army12 = Army.builder().name("2011").createdAt(OffsetDateTime.of(2011, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army13 = Army.builder().name("2012").createdAt(OffsetDateTime.of(2012, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army14 = Army.builder().name("2013").createdAt(OffsetDateTime.of(2013, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army15 = Army.builder().name("2014").createdAt(OffsetDateTime.of(2014, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army16 = Army.builder().name("2015").createdAt(OffsetDateTime.of(2015, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army17 = Army.builder().name("2016").createdAt(OffsetDateTime.of(2016, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army18 = Army.builder().name("2017").createdAt(OffsetDateTime.of(2017, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army19 = Army.builder().name("2018").createdAt(OffsetDateTime.of(2018, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        Army army20 = Army.builder().name("2019").createdAt(OffsetDateTime.of(2019, 1, 1,10,10,0,0, ZoneOffset.UTC)).isPaid(false).build();
        List<Army> armyList = List.of(army1,army2,army3,army4,army5,army6,army7,army8, army9,army10,army11,army12,army13,army14,army15,army16,army17,army18,army19,army20);
        when(mockArmyRepository.findAll()).thenReturn(armyList);

        var result = armyService.getUnpaid();

        assertThat(result.size()).isEqualTo(10);
        assertThat(result).isEqualTo(List.of(army1,army2,army3,army4,army5,army6,army7,army8,army9,army10));
    }

    @Test
    void ensureConvertUnitInputIntoUnitsWorksProperly() {
        log.debug("Testing if convertUnitInputIntoUnits works properly with correct values");

        String unitString = "Mounted Gondorian Ranger:5-Mordor Orc:3-Kek:50    ";

        var result = armyService.convertUnitInputIntoUnits(unitString);

        assertThat(result).isNotNull();
        assertThat(result.length).isEqualTo(3);
        assertThat(result[0].unitTypeName()).isEqualTo("Gondorian Ranger");
        assertThat(result[0].amount()).isEqualTo(5);
        assertThat(result[0].mounted()).isTrue();
        assertThat(result[1].unitTypeName()).isEqualTo("Mordor Orc");
        assertThat(result[1].amount()).isEqualTo(3);
        assertThat(result[2].unitTypeName()).isEqualTo("Kek");
        assertThat(result[2].amount()).isEqualTo(50);

        log.info("Test passed: ConvertUnitInputIntoUnits works properly with correct values");
    }

    @Test
    void ensureValidateUnitStringWorksCorrectly() {
        log.debug("Testing if validateUnitString works properly with correct values");

        String unitString = "UNit:2-Units:60";
        armyService.validateUnitString(unitString);

        log.info("Test passed: validateUnitString works properly with correct values");
    }

    @Test
    void ensureValidateUnitStringThrowsSeWhenNamePartIsWrong() {

        String unitString = "Un-It";
        assertThrows(ArmyServiceException.class, () -> armyService.validateUnitString(unitString));
        String unitString2 = "Unit:5s-";
        assertThrows(ArmyServiceException.class, () -> armyService.validateUnitString(unitString2));
        String unitString3 = "Unit:55-";
        assertThrows(ArmyServiceException.class, () -> armyService.validateUnitString(unitString3));

    }
}
