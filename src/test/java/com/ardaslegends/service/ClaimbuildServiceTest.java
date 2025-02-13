package com.ardaslegends.service;

import com.ardaslegends.domain.*;
import com.ardaslegends.repository.claimbuild.ClaimbuildRepository;
import com.ardaslegends.repository.ProductionSiteRepository;
import com.ardaslegends.repository.region.RegionRepository;
import com.ardaslegends.service.dto.claimbuild.CreateClaimBuildDto;
import com.ardaslegends.service.dto.claimbuilds.DeleteClaimbuildDto;
import com.ardaslegends.service.dto.claimbuilds.UpdateClaimbuildOwnerDto;
import com.ardaslegends.service.exceptions.ServiceException;
import com.ardaslegends.service.exceptions.logic.claimbuild.ClaimBuildServiceException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ClaimbuildServiceTest {

    private ClaimBuildService claimBuildService;
    private ClaimbuildRepository mockClaimbuildRepository;
    private RegionRepository mockRegionRepository;
    private ProductionSiteRepository mockProductionSiteRepository;
    private FactionService mockFactionService;
    private PlayerService mockPlayerService;

    private Faction faction;
    private Faction faction2;
    private ClaimBuild claimbuild;
    private ProductionSiteType productionSiteType;
    private ProductionSite productionSite;
    private ProductionSite productionSite2;
    private ProductionClaimbuild productionClaimbuild;
    private ProductionClaimbuild productionClaimbuild2;
    private SpecialBuilding specialBuilding;
    private SpecialBuilding specialBuilding2;
    private ClaimBuildType claimBuildType;
    private Coordinate coordinate;
    private String claimbuildName;
    private Region region;
    private Player player;
    private Player player2;
    private Player player3;
    private CreateClaimBuildDto createClaimBuildDto;

    @BeforeEach
    void setup() {
        mockClaimbuildRepository = mock(ClaimbuildRepository.class);
        mockRegionRepository = mock(RegionRepository.class);
        mockFactionService = mock(FactionService.class);
        mockProductionSiteRepository = mock(ProductionSiteRepository.class);
        mockPlayerService = mock(PlayerService.class);

        claimBuildService = new ClaimBuildService(mockClaimbuildRepository, mockRegionRepository, mockProductionSiteRepository, mockFactionService, mockPlayerService);

        claimbuildName= "Minas Tirith";
        var claimbuildId = 10L;

        faction = Faction.builder().name("Gondor").build();
        faction2 = Faction.builder().name("Mordor").build();
        player = Player.builder().ign("Luktronic").discordID("1234").faction(faction).build();
        player2 = Player.builder().ign("mirak441").discordID("567").faction(faction).build();
        player3 = Player.builder().ign("VernonRoche").discordID("8910").faction(faction).build();

        Resource fish = Resource.builder().id(10L).resourceName("Fish").build();
        Resource salmon = Resource.builder().id(11L).resourceName("Salmon").build();

        productionSiteType = ProductionSiteType.FISHING_LODGE;
        productionSite = ProductionSite.builder().id(1L).producedResource(fish).type(productionSiteType).build();
        productionSite2 = ProductionSite.builder().id(2L).producedResource(salmon).type(productionSiteType).build();
        productionClaimbuild = ProductionClaimbuild.builder().id(ProductionClaimbuildId.builder().claimbuildId(claimbuildId).productionSiteId(productionSite.getId()).build()).claimbuild(claimbuild).productionSite(productionSite).count(1L).build();
        productionClaimbuild2 = ProductionClaimbuild.builder().claimbuild(claimbuild).id(ProductionClaimbuildId.builder().claimbuildId(claimbuildId).productionSiteId(productionSite2.getId()).build()).claimbuild(claimbuild).productionSite(productionSite2).count(3L).build();
        specialBuilding = SpecialBuilding.EMBASSY;
        specialBuilding2 = SpecialBuilding.HOUSE_OF_HEALING;
        claimBuildType = ClaimBuildType.TOWN;
        coordinate = Coordinate.builder().x(120).y(69).z(420).build();
        region = Region.builder().id("92").regionType(RegionType.HILL).claimedBy(Set.of(faction)).build();
        claimbuild = ClaimBuild.builder().name(claimbuildName).ownedBy(faction).type(claimBuildType).coordinates(coordinate).region(region)
                .traders("Gondor Blacksmith").siege("Trebuchet").numberOfHouses("14 Small House")
                .specialBuildings(List.of(specialBuilding, specialBuilding2)).builtBy(Set.of(player, player2, player3))
                .productionSites(List.of(productionClaimbuild, productionClaimbuild2))
                .createdArmies(new ArrayList<>()).stationedArmies(new ArrayList<>()).build();

        productionClaimbuild.setClaimbuild(claimbuild);
        productionClaimbuild2.setClaimbuild(claimbuild);
        region.setClaimBuilds(Set.of(claimbuild));

        createClaimBuildDto = new CreateClaimBuildDto(claimbuild.getName(), region.getId(), claimBuildType.name().toLowerCase(), faction.getName(),
                coordinate.getX(), coordinate.getY(), coordinate.getZ(),
                "%s:%s:%d-%s:%s:%d".formatted(productionSite.getType(), productionSite.getProducedResource().getResourceName(), productionClaimbuild.getCount(), productionSite2.getType(), productionSite2.getProducedResource().getResourceName(), productionClaimbuild2.getCount()),
                "%s-%s".formatted(specialBuilding.name(), specialBuilding2.name()), claimbuild.getTraders(), claimbuild.getSiege(), claimbuild.getNumberOfHouses(),
                "%s-%s-%s".formatted(player.getIgn(), player2.getIgn(), player3.getIgn()));

        when(mockClaimbuildRepository.findClaimBuildByName(claimbuild.getName())).thenReturn(Optional.of(claimbuild));
        when(mockFactionService.getFactionByName(faction.getName())).thenReturn(faction);
        when(mockFactionService.getFactionByName(faction2.getName())).thenReturn(faction2);
        when(mockProductionSiteRepository.
                queryByTypeAndResourceOptional(productionSiteType, productionSite.getProducedResource().getResourceName()))
                .thenReturn(Optional.of(productionSite));
        when(mockProductionSiteRepository.
                queryByTypeAndResourceOptional(productionSiteType, productionSite2.getProducedResource().getResourceName()))
                .thenReturn(Optional.of(productionSite2));
        when(mockPlayerService.getPlayerByIgn(player.getIgn())).thenReturn(player);
        when(mockPlayerService.getPlayerByIgn(player2.getIgn())).thenReturn(player2);
        when(mockPlayerService.getPlayerByIgn(player3.getIgn())).thenReturn(player3);
        when(mockRegionRepository.findById(region.getId())).thenReturn(Optional.of(region));
        when(mockClaimbuildRepository.save(claimbuild)).thenReturn(claimbuild);
    }

    @Test
    void ensureSetOwnerFactionWorksProperly() {
        log.debug("Testing if setOwnerFaction works properly with correct values");

        UpdateClaimbuildOwnerDto dto = new UpdateClaimbuildOwnerDto(claimbuild.getName(), faction2.getName());

        when(mockClaimbuildRepository.save(claimbuild)).thenReturn(claimbuild);

        log.debug("Calling setOwnerFaction, expecting no errors");
        var result = claimBuildService.changeOwnerFromDto(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(dto.claimbuildName());
        assertThat(result.getOwnedBy().getName()).isEqualTo(dto.newFaction());

        log.info("Test passed: setOwnerFaction works properly with correct values");
    }

    @Test
    void ensureDeleteClaimbuildWorksProperly() {
        log.debug("Testing if deleteClaimbuild works properly");

        DeleteClaimbuildDto dto = new DeleteClaimbuildDto(claimbuild.getName(), null, null);

        var claimedBy = new HashSet<Faction>();
        claimedBy.add(faction);
        region.setClaimedBy(claimedBy);
        var claimbuilds = new HashSet<ClaimBuild>();
        region.setClaimBuilds(claimbuilds);
        var regions = new HashSet<Region>();
        faction.setRegions(regions);

        log.debug("Calling deleteClaimbuild, expecting no errors");
        var result = claimBuildService.deleteClaimbuild(dto);

        assertThat(result.getName()).isEqualTo(claimbuild.getName());
        log.info("Test passed: deleteClaimbuild works properly");
    }

    @Test
    void ensureGetClaimbuildByNameThrowsSeWhenPassedNameDoesNotHaveACb() {
        log.debug("Testing if getClaimbuildByName throws Se when passed name does not have a corresponding claimbuild in database");

        String name = "Kek";
        when(mockClaimbuildRepository.findClaimBuildByName(name)).thenReturn(Optional.empty());

        log.debug("Calling getClaimbuildByName, expecting Se");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.getClaimBuildByName(name));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.noCbWithName(name).getMessage());
        log.info("Test passed: getClaimbuildByName correctly throws Se when no cb entry with passed name in database");
    }

    // CREATE CLAIMBUILD

    @Test
    void ensureCreateClaimbuildWorks() {
        log.debug("Testing if createClaimbuild works properly");

        when(mockClaimbuildRepository.findClaimBuildByName(claimbuild.getName())).thenReturn(Optional.empty());
        when(mockClaimbuildRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        log.debug("Calling createClaimbuild");
        ClaimBuild result = claimBuildService.createClaimbuild(createClaimBuildDto, true);

        assertThat(result.getName()).isEqualTo(claimbuildName);
        assertThat(result.getRegion()).isEqualTo(region);
        assertThat(result.getType()).isEqualTo(claimBuildType);
        assertThat(result.getOwnedBy()).isEqualTo(faction);
        assertThat(result.getCoordinates()).isEqualTo(coordinate);
        assertThat(result.getProductionSites().containsAll(List.of(productionClaimbuild, productionClaimbuild2))).isTrue();
        assertThat(result.getSpecialBuildings().containsAll(List.of(specialBuilding, specialBuilding2))).isTrue();
        assertThat(result.getTraders()).isEqualTo(claimbuild.getTraders());
        assertThat(result.getSiege()).isEqualTo(claimbuild.getSiege());
        assertThat(result.getNumberOfHouses()).isEqualTo(claimbuild.getNumberOfHouses());
        assertThat(result.getBuiltBy().containsAll(List.of(player, player2, player3))).isTrue();
        log.info("Test passed: createClaimbuild works properly");
    }

    @Test
    void ensureCreateClaimbuildWorksWithUpdatedClaimbuild() {
        log.debug("Testing if createClaimbuild works properly when updating claimbuild");

        when(mockClaimbuildRepository.findClaimBuildByName(claimbuild.getName())).thenReturn(Optional.of(claimbuild));
        when(mockClaimbuildRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        createClaimBuildDto = new CreateClaimBuildDto(claimbuild.getName(), region.getId(), ClaimBuildType.CAPITAL.name().toLowerCase(), faction.getName(),
                coordinate.getX(), coordinate.getY(), coordinate.getZ(),
                "%s:%s:%d-%s:%s:%d".formatted(productionSite.getType(), productionSite.getProducedResource().getResourceName(), productionClaimbuild.getCount(), productionSite2.getType(), productionSite2.getProducedResource().getResourceName(), productionClaimbuild2.getCount()),
                "%s-%s".formatted(specialBuilding.name(), specialBuilding2.name()), claimbuild.getTraders(), claimbuild.getSiege(), claimbuild.getNumberOfHouses(),
                "%s".formatted(player.getIgn()));

        log.debug("Calling createClaimbuild");
        ClaimBuild result = claimBuildService.createClaimbuild(createClaimBuildDto, false);

        assertThat(result).isEqualTo(claimbuild);
        assertThat(result.getType()).isEqualTo(ClaimBuildType.CAPITAL);
        assertThat(result.getBuiltBy().size()).isEqualTo(1);
        assertThat(result.getBuiltBy().contains(player)).isTrue();
        log.info("Test passed: createClaimbuild works properly when updating claimbuild");
    }

    @Test
    void ensureCreateClaimbuildThrowsSeWhenCBAlreadyExists() {
        log.debug("Testing if createClaimbuild throws ClaimBuildServiceException when claimbuild already exists!");

        log.debug("Calling createClaimbuild");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.createClaimbuild(createClaimBuildDto, true));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.cbAlreadyExists(claimbuildName, region.getId(), faction.getName()).getMessage());
        log.info("Test passed: createClaimbuild throws ClaimBuildServiceException when claimbuild already exists!");
    }

    @Test
    void ensureCreateClaimbuildThrowsSeWhenFactionAlreadyHasCapital() {
        log.debug("Testing if createClaimbuild throws ClaimBuildServiceException when faction already has capital!");

        claimbuild.setType(ClaimBuildType.CAPITAL);
        createClaimBuildDto = new CreateClaimBuildDto(claimbuild.getName(), region.getId(), ClaimBuildType.CAPITAL.name().toLowerCase(), faction.getName(),
                coordinate.getX(), coordinate.getY(), coordinate.getZ(),
                "%s:%s:%d-%s:%s:%d".formatted(productionSite.getType(), productionSite.getProducedResource(), productionClaimbuild.getCount(), productionSite2.getType(), productionSite2.getProducedResource(), productionClaimbuild2.getCount()),
                "%s-%s".formatted(specialBuilding.name(), specialBuilding2.name()), claimbuild.getTraders(), claimbuild.getSiege(), claimbuild.getNumberOfHouses(),
                "%s".formatted(player.getIgn()));
        when(mockClaimbuildRepository.findClaimBuildByName(claimbuild.getName())).thenReturn(Optional.empty());
        faction.setClaimBuilds(List.of(ClaimBuild.builder().type(ClaimBuildType.CAPITAL).build()));

        log.debug("Calling createClaimbuild");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.createClaimbuild(createClaimBuildDto, true));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.factionAlreadyHasCapital(faction.getName()).getMessage());
        log.info("Test passed: createClaimbuild throws ClaimBuildServiceException when faction already has capital!");
    }

    @Test
    void ensureCreateClaimbuildThrowsSeWhenUpdatedCBDoesNotExists() {
        log.debug("Testing if createClaimbuild throws ClaimBuildServiceException when updated claimbuild does not exist!");

        when(mockClaimbuildRepository.findClaimBuildByName(claimbuild.getName())).thenReturn(Optional.empty());

        log.debug("Calling createClaimbuild");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.createClaimbuild(createClaimBuildDto, false));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.couldNotUpdateClaimbuildBecauseNotFound(claimbuildName).getMessage());
        log.info("Test passed: createClaimbuild throws ClaimBuildServiceException when updated claimbuild does not exist!");
    }

    @Test
    void ensureCreateClaimbuildThrowsSeWhenRegionDoesNotExist() {
        log.debug("Testing if createClaimbuild throws ServiceException when region does not exists!");

        when(mockClaimbuildRepository.findClaimBuildByName(claimbuild.getName())).thenReturn(Optional.empty());
        when(mockRegionRepository.findById(region.getId())).thenReturn(Optional.empty());

        log.debug("Calling createClaimbuild");
        var result = assertThrows(ServiceException.class, () -> claimBuildService.createClaimbuild(createClaimBuildDto, true));

        assertThat(result.getMessage()).isEqualTo(ServiceException.regionDoesNotExist(region.getId()).getMessage());
        log.info("Test passed: createClaimbuild throws ServiceException when region does not exists!");
    }

    @Test
    void ensureCreateClaimbuildThrowsSeWhenCbTypeDoesNotExist() {
        log.debug("Testing if createClaimbuild throws ClaimBuildServiceException when ClaimBuildType does not exists!");

        when(mockClaimbuildRepository.findClaimBuildByName(claimbuild.getName())).thenReturn(Optional.empty());

        createClaimBuildDto = new CreateClaimBuildDto(claimbuild.getName(), region.getId(), "ajhwdhjahd", faction.getName(),
                coordinate.getX(), coordinate.getY(), coordinate.getZ(),
                "%s:%s:%d-%s:%s:%d".formatted(productionSite.getType(), productionSite.getProducedResource(), productionClaimbuild.getCount(), productionSite2.getType(), productionSite2.getProducedResource(), productionClaimbuild2.getCount()),
                "%s-%s".formatted(specialBuilding.name(), specialBuilding2.name()), claimbuild.getTraders(), claimbuild.getSiege(), claimbuild.getNumberOfHouses(),
                "%s-%s-%s".formatted(player.getIgn(), player2.getIgn(), player3.getIgn()));


        log.debug("Calling createClaimbuild");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.createClaimbuild(createClaimBuildDto, true));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.noCbTypeFound(createClaimBuildDto.type()).getMessage());
        log.info("Test passed: createClaimbuild throws ClaimBuildServiceException when ClaimBuildType does not exists!");
    }

    @Test
    void ensureCreateProductionSitesFromStringWorks() {
        log.debug("Testing if createProductionSitesFromString works properly");

        String prodString = "Fishing Lodge:Fish:5";

        log.debug("Calling createProductionSitesFromString");
        List<ProductionClaimbuild> result = claimBuildService.createProductionSitesFromString(prodString, claimbuild);

        ProductionClaimbuild expected = ProductionClaimbuild.builder().claimbuild(claimbuild).productionSite(productionSite)
                .count(5L).build();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getClaimbuild()).isEqualTo(expected.getClaimbuild());
        assertThat(result.get(0).getProductionSite()).isEqualTo(expected.getProductionSite());
        assertThat(result.get(0).getCount()).isEqualTo(expected.getCount());
        log.info("Test passed: createProductionSitesFromString works properly");
    }

    @Test
    void ensureCreateProductionSitesFromStringThrowsCBSEWhenNoProdTypeFound() {
        log.debug("Testing if createProductionSitesFromString throws ClaimBuildServiceException when no ProductionSiteType found!");

        String str = "kekw Lodge";
        String prodString = str + ":Fish:5";

        log.debug("Calling createProductionSitesFromString");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.createProductionSitesFromString(prodString, claimbuild));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.noProductionSiteTypeFound(str).getMessage());
        log.info("Test passed: createProductionSitesFromString throws ClaimBuildServiceException when no ProductionSiteType found!");
    }

    @Test
    void ensureCreateProductionSitesFromStringThrowsCBSEWhenNoProdSiteFound() {
        log.debug("Testing if createProductionSitesFromString throws ClaimBuildServiceException when no ProductionSite found!");

        String type = "Fishing Lodge";
        String resource = "Almond";
        String prodString = "%s:%s:5".formatted(type, resource);

        log.debug("Calling createProductionSitesFromString");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.createProductionSitesFromString(prodString, claimbuild));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.noProductionSiteFound(productionSiteType.name(), resource).getMessage());
        log.info("Test passed: createProductionSitesFromString throws ClaimBuildServiceException when no ProductionSite found!");
    }

    @Test
    void ensureCreateProductionSitesFromStringThrowsCBSEWhenAmountNotANumber() {
        log.debug("Testing if createProductionSitesFromString throws ClaimBuildServiceException when inputted amount is not a number!");

        String prodString = "Fishing Lodge:Fish:awdada";

        log.debug("Calling createProductionSitesFromString");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.createProductionSitesFromString(prodString, claimbuild));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.invalidProductionSiteString(prodString).getMessage());
        log.info("Test passed: createProductionSitesFromString throws ClaimBuildServiceException when inputted amount is not a number!");
    }

    @Test
    void ensureCreateBuiltByFromStringWorks() {
        log.debug("Testing if createBuiltByFromString works properly");

        String builtByString = "%s-%s-%s".formatted(player.getIgn(), player2.getIgn(), player3.getIgn());

        log.debug("Calling createBuiltByFromString");
        Set<Player> result = claimBuildService.createBuiltByFromString(builtByString);

        Set<Player> expected = Set.of(player, player2, player3);

        assertThat(result.size()).isEqualTo(expected.size());
        assertThat(result.containsAll(expected)).isTrue();
        log.info("Test passed: createBuiltByFromString works properly");
    }

    @Test
    void ensureCreateSpecialBuildingsFromStringWorks() {
        log.debug("Testing if createSpecialBuildingsFromString works properly");

        String specialBuildingString = "House of Healing-Watchtower-Embassy";

        log.debug("Calling createSpecialBuildingsFromString");
        List<SpecialBuilding> result = claimBuildService.createSpecialBuildingsFromString(specialBuildingString);

        List<SpecialBuilding> expected = List.of(SpecialBuilding.HOUSE_OF_HEALING, SpecialBuilding.WATCHTOWER, SpecialBuilding.EMBASSY);

        assertThat(result.size()).isEqualTo(expected.size());
        assertThat(result.containsAll(expected)).isTrue();
        log.info("Test passed: createSpecialBuildingsFromString works properly");
    }

    @Test
    void ensureCreateSpecialBuildingsFromStringThrowsSEWhenNoBuildFound() {
        log.debug("Testing if createSpecialBuildingsFromString throws ClaimBuildServiceException when no Special Building is found");


        String specialBuildingString = "House awdada";

        log.debug("Calling createSpecialBuildingsFromString");
        var result = assertThrows(ClaimBuildServiceException.class, () -> claimBuildService.createSpecialBuildingsFromString(specialBuildingString));

        assertThat(result.getMessage()).isEqualTo(ClaimBuildServiceException.noSpecialBuildingFound(specialBuildingString).getMessage());
        log.info("Test passed: createSpecialBuildingsFromString throws ClaimBuildServiceException when no Special Building is found");
    }
}
