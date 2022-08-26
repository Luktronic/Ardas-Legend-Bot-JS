package com.ardaslegends.data.service;

import com.ardaslegends.data.domain.*;
import com.ardaslegends.data.repository.ArmyRepository;
import com.ardaslegends.data.repository.ClaimBuildRepository;
import com.ardaslegends.data.repository.MovementRepository;
import com.ardaslegends.data.service.dto.army.*;
import com.ardaslegends.data.service.dto.unit.UnitTypeDto;
import com.ardaslegends.data.service.exceptions.FactionServiceException;
import com.ardaslegends.data.service.exceptions.army.ArmyServiceException;
import com.ardaslegends.data.repository.FactionRepository;
import com.ardaslegends.data.service.exceptions.claimbuild.ClaimBuildServiceException;
import com.ardaslegends.data.service.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class ArmyService extends AbstractService<Army, ArmyRepository> {
    private final ArmyRepository armyRepository;
    private final MovementRepository movementRepository;
    private final PlayerService playerService;
    private final FactionRepository factionRepository;
    private final UnitTypeService unitTypeService;
    private final ClaimBuildRepository claimBuildRepository;

    @Transactional(readOnly = false)
    public Army createArmy(CreateArmyDto dto) {
        log.debug("Creating army with data [{}]", dto);

        ServiceUtils.checkNulls(dto, List.of("executorDiscordId", "name", "armyType", "claimBuildName", "units"));
        ServiceUtils.checkBlanks(dto, List.of("executorDiscordId", "name", "claimBuildName"));
        Arrays.stream(dto.units()).forEach(ServiceUtils::checkAllBlanks);

        log.debug("Fetching required Data");

        log.trace("Fetching if an army with name [{}] already exists.", dto.name());
        Optional<Army> fetchedArmy = secureFind(dto.name(), armyRepository::findById);

        if(fetchedArmy.isPresent()) {
            log.warn("Army with name [{}] already exists", dto.name());
            throw new IllegalArgumentException("Army with name %s already exists, please choose a different name".formatted(dto.name()));
        }


        log.trace("Fetching Executor by discordId [{}]", dto.executorDiscordId());
        Player fetchedPlayer = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        log.debug("Assembling Units Map, fetching units");
        var unitTypes = Arrays.stream(dto.units())
                .collect(Collectors.toMap(
                        unitTypeDto -> unitTypeService.getUnitTypeByName(unitTypeDto.unitTypeName()), // Key
                        UnitTypeDto::amount, // Value
                        ((integer, integer2) -> integer + integer2) // Merge function, called on duplicate key
                ));

        log.trace("Fetching Claimbuild with name [{}]", dto.claimBuildName());
        Optional<ClaimBuild> fetchedClaimbuild = secureFind(dto.claimBuildName(), claimBuildRepository::findById);

        if(fetchedClaimbuild.isEmpty()) {
            log.warn("No ClaimBuild found with name [{}]", dto.claimBuildName());
            throw ClaimBuildServiceException.noCbWithName(dto.claimBuildName());
        }

        ClaimBuild inputClaimBuild = fetchedClaimbuild.get();

        log.trace("Finished fetching required data");

        log.debug("Checking if the Player has the same faction as the Claimbuild");
        if(!inputClaimBuild.getOwnedBy().equals(fetchedPlayer.getFaction())) {
            log.warn("Player [{}] and Claimbuild [{}] not in the same faction!");
            throw ArmyServiceException.cannotCreateArmyFromClaimbuildInDifferentFaction(fetchedPlayer.getFaction().getName(), inputClaimBuild.getOwnedBy().getName(), ArmyType.ARMY);
        }

        log.debug("Checking if ClaimBuild can create another army");

        if(inputClaimBuild.atMaxArmies()) {
            log.warn("ClaimBuild [{}] already has max amount of armies created! Armies {}",inputClaimBuild ,inputClaimBuild.getCreatedArmies() );
            throw ArmyServiceException.maxArmyOrCompany(ArmyType.ARMY,inputClaimBuild.toString(), inputClaimBuild.getCreatedArmies().toString());
        }

        log.debug("Checking if token count does not exceed 30");
        // Calculate the tokens that were used
        log.trace("Calculating amount of tokens used");
        double tokenCount = 0;
        List<Unit> units = new ArrayList<>();
        for (UnitType unit: unitTypes.keySet()) {
            // Get the UnitType cost and multiply it by the count of that unit, which is stored in the units Map
            tokenCount += unit.getTokenCost() * unitTypes.get(unit);
            units.add(new Unit(null, unit, null, unitTypes.get(unit),unitTypes.get(unit)));
        }

        log.debug("Calculated Token count is [{}]", tokenCount);
        if(tokenCount > 30) {
            log.warn("Token count exceeds 30 [{}]", tokenCount);
            throw ArmyServiceException.tooHighTokenCount(dto.armyType(),tokenCount);
        }

        boolean isPaid = false;

        if(inputClaimBuild.getFreeArmiesRemaining() > 0) {
            log.debug("Claimbuild [{}] has free armies remaining [{}]. Decrementing value and setting isPaid to true",
                    inputClaimBuild.getName(), inputClaimBuild.getFreeArmiesRemaining());
            inputClaimBuild.setFreeArmiesRemaining(inputClaimBuild.getFreeArmiesRemaining() - 1);
            isPaid = true;
        }


        // TODO: Relay info if army is free or costs something

        log.trace("Assembling Army Object");
        Army army = new Army(dto.name(),
                dto.armyType(),
                fetchedPlayer.getFaction(),
                inputClaimBuild.getRegion(),
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                inputClaimBuild,
                30-tokenCount,
                false,
                inputClaimBuild,
                LocalDateTime.now(),
                isPaid);

        log.trace("Adding the army to each unit");
        Army finalArmy = army;
        units.forEach(unit -> unit.setArmy(finalArmy));

        army.setUnits(units);

        log.info("BEFORE SAVE How many armies are in createdArmies [{}]", inputClaimBuild.getCreatedArmies().size());


        log.debug("Trying to persist the army object");
        army = secureSave(army, armyRepository);

        log.info("AFTER SAVE How many armies are in createdArmies [{}] ", inputClaimBuild.getCreatedArmies().size());
        log.info("Successfully created army [{}]!", army.getName());
        return army;
    }

    @Transactional(readOnly = false)
    public Army healStart(UpdateArmyDto dto) {
        log.debug("Trying to start healing for army [{}]", dto.armyName());

        ServiceUtils.checkNulls(dto, List.of("executorDiscordId", "armyName"));
        ServiceUtils.checkBlanks(dto, List.of("executorDiscordId", "armyName"));

        log.debug("Fetching required data");

        log.trace("Fetching Player");
        Player player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        log.trace("Fetching Army");
        Army army = getArmyByName(dto.armyName());

        log.debug("Checking if army object is an army or armed company");
        if(!ArmyType.ARMY.equals(army.getArmyType()) && !ArmyType.ARMED_TRADERS.equals(army.getArmyType())) {
            log.warn("Army object [{}] is not a armed company or army: [{}]", army.getName(), army.getArmyType().getName());
            throw ArmyServiceException.tradingCompaniesCannotHeal(army.getName());
        }

        log.debug("Checking if army and player are in the same faction");
        if(!player.getFaction().equals(army.getFaction())) {
            log.warn("Player [{}:{}]and Army [{}:{}] are not in the same faction ", player, player.getFaction(), army, army.getFaction());
            throw ArmyServiceException.armyAndPlayerInDifferentFaction(army.getArmyType(), player.getFaction().toString(), army.getFaction().toString());
        }

        log.debug("Checking if army has dead units");
        if(army.allUnitsAlive()) {
            log.warn("Army [{}] is already fully healed!");
            throw ArmyServiceException.alreadyFullyHealed(army.getArmyType(), army.getName());
        }

        log.debug("Checking if army is stationed at a CB");
        // Army is null or Claimbuild does not have a House of healing
        if(army.getStationedAt() == null || !army.getStationedAt().getSpecialBuildings().contains(SpecialBuilding.HOUSE_OF_HEALING)) {
            log.warn("Army [{}] is not stationed at a claimbuild");
            throw ArmyServiceException.needToStationArmyAtCbWithHouseOfHealing(army.getArmyType(), army.toString());
        }

        log.debug("All checks validated, persisting now");

        army.setHealing(true);
        army = secureSave(army, armyRepository);

        log.info("Army [{}] is now healing", army.toString());
        return army;
    }

    @Transactional(readOnly = false)
    public Army healStop(UpdateArmyDto dto) {
        log.debug("Trying to start healing for army [{}]", dto.armyName());

        ServiceUtils.checkNulls(dto, List.of("executorDiscordId", "armyName"));
        ServiceUtils.checkBlanks(dto, List.of("executorDiscordId", "armyName"));

        log.trace("Fetching Player");
        Player player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        log.trace("Fetching Army");
        Army army = getArmyByName(dto.armyName());

        log.debug("Checking if army is healing");
        if(!army.isHealing()) {
            log.warn("Army [{}] is not healing, cant stop it!");
            throw ArmyServiceException.armyIsNotHealing(army.getArmyType(), army.toString());
        }

        log.debug("Checking if army and player are in the same faction");
        if(!player.getFaction().equals(army.getFaction())) {
            log.warn("Player [{}:{}]and Army [{}:{}] are not in the same faction ", player, player.getFaction(), army, army.getFaction());
            throw ArmyServiceException.armyAndPlayerInDifferentFaction(army.getArmyType(), player.getFaction().toString(), army.getFaction().toString());
        }

        army.setHealing(false);
        army = secureSave(army, armyRepository);

        log.info("Army [{}] now stopped healing", army.toString());
        return army;
    }

    @Transactional(readOnly = false)
    public Army bind(BindArmyDto dto) { //TODO Change to Army.bind()
        log.debug("Binding army [{}] to player with discord id [{}]", dto.armyName(), dto.targetDiscordId());

        log.trace("Validating data");
        ServiceUtils.checkAllNulls(dto);
        ServiceUtils.checkAllBlanks(dto);

        log.trace("Getting the executor player's instance");
        Player executor = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        /*
        Checking if the executor is the faction leader - if not, throw error that player doesn't have permission to bind
         */
        boolean isBindingSelf = dto.executorDiscordId().equals(dto.targetDiscordId()); //Says if the player is binding themselves

        log.debug("Checking if executor and target are not equal");
        if (!isBindingSelf) {
            //TODO Check for lords as well
            log.trace("Executor and target are not equal - checking if executor is faction leader");
            if (!executor.equals(executor.getFaction().getLeader())) {
                log.warn("Executor player [{}] is not faction leader of faction [{}]!", executor, executor.getFaction());
                throw ArmyServiceException.notFactionLeader(executor.getFaction().getName());
            }
        }

        /*
        Setting target player
        If not binding self then fetch the target player from DB
         */
        log.trace("Getting the target player's instance");
        Player targetPlayer = null;
        if (isBindingSelf)
            targetPlayer = executor;
        else
            targetPlayer = playerService.getPlayerByDiscordId(dto.targetDiscordId());

        Army army = getArmyByName(dto.armyName());

        // TODO: Check for Wanderer or Allied Faction
        log.debug("Checking if army and player are in the same faction");
        if (!army.getFaction().equals(targetPlayer.getFaction())) {
            log.debug("Player and army are not in the same faction");
            log.debug("Checking if the faction is allied to the players faction");
            //TODO Check for allied faction
            log.debug("Checking if the player is a wanderer");
            if(targetPlayer.getFaction().getName().equals("Wanderer")) {
                log.debug("Target player is a wanderer - checking if executor is faction leader");
                //TODO Check for Lords as well
                if(!executor.equals(executor.getFaction().getLeader())) {
                    log.warn("Player [{}] is not faction leader of [{}] and therefore cannot bind wanderers!", executor, executor.getFaction());
                    throw ArmyServiceException.onlyLeaderCanBindWanderer(army.getArmyType());
                }
            }
            else {
                log.warn("Army [{}] and player [{}] are not in the same faction (army: [{}], player: [{}])", army.getName(), targetPlayer, army.getFaction(), targetPlayer.getFaction());
                throw ArmyServiceException.notSameFaction(army.getArmyType(), army.getName(), targetPlayer.getFaction().getName(), army.getFaction().getName());
            }
        }

        log.debug("Checking if army and player are in the same region");
        if (!army.getCurrentRegion().equals(targetPlayer.getRpChar().getCurrentRegion())) {
            log.warn("Army and player are not in the same region!");
            throw ArmyServiceException.notInSameRegion(army.getArmyType(), army.getName(), targetPlayer.getRpChar().getName());
        }

        log.debug("Checking if army is already bound to player");
        if (army.getBoundTo() != null) {
            log.warn("Army [{}] is already bound to another player [{}]!", army.getName(), army.getBoundTo());
            throw ArmyServiceException.alreadyBound(army.getArmyType(), army.getName(), army.getBoundTo().getIgn());
        }

        log.debug("Checking if army is in an active movement");
        Optional<Movement> armyActiveMove = movementRepository.findMovementByArmyAndIsCurrentlyActiveTrue(army);
        if(armyActiveMove.isPresent()) {
            String destinationRegion =  armyActiveMove.get().getPath().getDestination();
            log.warn("Army [{}] is currently moving to region [{}] and therefore cannot be bound to player [{}]!", army.getName(), destinationRegion, targetPlayer);
            throw ArmyServiceException.cannotBindArmyIsMoving(army.getArmyType(), army.getName(), destinationRegion);
        }

        log.debug("Checking if rp char is in an active movement");
        Optional<Movement> charActiveMove = movementRepository.findMovementByPlayerAndIsCurrentlyActiveTrue(targetPlayer);
        if(charActiveMove.isPresent()) {
            String destinationRegion =  charActiveMove.get().getPath().getDestination();
            log.warn("Character [{}] is currently moving to region [{}] and therefore cannot be bound to army [{}]!", targetPlayer, destinationRegion, army.getName());
            throw ArmyServiceException.cannotBindCharIsMoving(army.getArmyType(),army.getName(), destinationRegion);
        }

        log.debug("Binding army [{}] to player [{}]...", army.getName(), targetPlayer);
        army.setBoundTo(targetPlayer);
        targetPlayer.getRpChar().setBoundTo(army);

        log.debug("Persisting newly changed army...");
        army = secureSave(army, armyRepository);

        log.info("Bound {} [{}] to player [{}]!", army.getArmyType().name(), army.getName(), targetPlayer);
        return army;
    }

    @Transactional(readOnly = false)
    public Army unbind(BindArmyDto dto) {
        log.debug("Unbinding army [{}] from player [{}] - executed by player [{}]", dto.armyName(), dto.targetDiscordId(), dto.executorDiscordId());

        log.trace("Validating data...");
        ServiceUtils.checkNulls(dto, List.of("armyName", "executorDiscordId"));
        ServiceUtils.checkBlanks(dto, List.of("armyName", "executorDiscordId"));

        log.trace("Calling playerService to get executor's instance");
        Player executor = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        /*
        Getting the army and checking if the army has a player bound to it
         */
        log.debug("Getting the army object");

        log.trace("Fetching the army by name");
        Army army = getArmyByName(dto.armyName());

        log.debug("Getting the currently bound player");

        Player boundPlayer = army.getBoundTo();
        log.trace("Checking if the army has a player bound to it");
        if(boundPlayer == null) {
            log.warn("There is no player bound to the army [{}]", army);
            throw ArmyServiceException.noPlayerBoundToArmy(army.getArmyType(), army.getName());
        }

        /*
        Checking if executor has permission to unbind player
        If target player is not the executor - check if executor is leader or lord
         */

        log.debug("Checking if executor has permission to unbind target player");

        log.trace("Checking if player is unbinding themselves");
        boolean isUnbindingSelf = executor.getDiscordID().equals(dto.targetDiscordId());

        if(!isUnbindingSelf) {
            log.debug("Executor is unbinding another player - checking if executor has permission to do so");
            //TODO check for lord as well
            if(!executor.equals(executor.getFaction().getLeader())) { //checking if executor is leader
                log.warn("Executor player [{}] is not faction leader or lord of [{}] and therefore cannot unbind other players!", executor, executor.getFaction());
                throw ArmyServiceException.notFactionLeader(executor.getFaction().getName());
            }
        }

        /*
        Unbind and save player
         */

        log.debug("Unbinding player and saving changes...");

        log.trace("Setting bound player to null");
        army.setBoundTo(null);
        boundPlayer.getRpChar().setBoundTo(null);

        log.trace("Persisting army");
        army = secureSave(army, armyRepository);

        log.info("Unbound player [{}] from army [{}] (faction [{}])", boundPlayer, army, army.getFaction());
        return army;
    }

    @Transactional(readOnly = false)
    public Army station(StationDto dto) {
        log.debug("Trying to station army [{}] at [{}]", dto.armyName(), dto.claimbuildName());

        log.trace("Validating data");
        ServiceUtils.checkAllNulls(dto);
        ServiceUtils.checkAllBlanks(dto);

        log.trace("Fetching army instance");
        Army army = getArmyByName(dto.armyName());

        log.trace("Fetching player instance");
        Player player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        log.trace("Fetching claimbuild)");
        Optional<ClaimBuild> optionalClaimBuild = secureFind(dto.claimbuildName(), claimBuildRepository::findById);

        if(optionalClaimBuild.isEmpty()) {
            log.warn("Claimbuild with name [{}] does not exist in database", dto.claimbuildName());
            throw ClaimBuildServiceException.noCbWithName(dto.claimbuildName());
        }

        ClaimBuild claimBuild = optionalClaimBuild.get();

        log.debug("Check if army is already stationed");
        if(army.getStationedAt() != null) {
            log.warn("Army [{}] is already stationed at Claimbuild [{}]", army.getName(), claimBuild.getName());
            throw ArmyServiceException.armyAlreadyStationed(army.getArmyType(), army.getName(), army.getStationedAt().getName());
        }

        // TODO: Check ally system
        log.debug("Checking if Claimbuild is in the same or an allied faction of the army");
        if(!claimBuild.getOwnedBy().equals(army.getFaction()) && !army.getFaction().getAllies().contains(claimBuild.getOwnedBy())) {
            log.warn("Claimbuild is not in the same or allied faction of the army");
            throw ArmyServiceException.claimbuildNotInTheSameOrAlliedFaction(army.getArmyType(), claimBuild.getName());
        }

        log.debug("Checking if executor is allowed to perform the movement");
        boolean isAllowed = ServiceUtils.boundLordLeaderPermission(player, army);

        log.debug("Is player [{}] allowed to perform station?: {}", player.getIgn(), isAllowed);
        if(!isAllowed) {
            log.warn("Player [{}] is not allowed to perform station", player.getIgn());
            throw ArmyServiceException.noPermissionToPerformThisAction();
        }

        army.setStationedAt(claimBuild);

        log.debug("Set stationed, performing persist");
        secureSave(army, armyRepository);

        log.info("Station Army Service Method for Army [{}] completed successfully");
        return army;
    }

    @Transactional(readOnly = false)
    public Army unstation(StationDto dto) {
        log.debug("Trying to unstation army with data: [{}]", dto);

        ServiceUtils.checkNulls(dto, List.of("executorDiscordId", "armyName"));
        ServiceUtils.checkBlanks(dto, List.of("executorDiscordId", "armyName"));

        log.trace("Fetching army instance");
        Army army = getArmyByName(dto.armyName());

        log.trace("Fetching player instance");
        Player player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        if(army.getStationedAt() == null) {
            log.warn("Army [{}] is not stationed at a cb, so cannot be unstationed!", army.toString());
            throw ArmyServiceException.armyNotStationed(army.getArmyType(), army.toString());
        }

        boolean isAllowed = ServiceUtils.boundLordLeaderPermission(player, army);

        if(!isAllowed) {
            log.warn("Player not does not have permission to perform unstation");
            throw ArmyServiceException.noPermissionToPerformThisAction();
        }

        log.debug("Player [{}] is allowed to perform unstation");

        army.setStationedAt(null);

        log.debug("Unstationed army [{}], persisting");
        secureSave(army, armyRepository);

        log.info("Unstation Army Service Method for Army [{}] completed successfully");
        return army;
    }
    @Transactional(readOnly = false)
    public Army disband(DeleteArmyDto dto, boolean forced) {
        log.debug("Trying to disband army [{}] executed by player [{}]", dto.armyName(), dto.executorDiscordId());

        log.trace("Validating data");
        ServiceUtils.checkAllNulls(dto);
        ServiceUtils.checkAllBlanks(dto);

        log.trace("Getting the army instance");
        Army army = getArmyByName(dto.armyName());

        log.trace("Getting the player instance");
        Player player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        log.debug("Checking if the faction of player and army are same");
        if(!forced && !player.getFaction().equals(army.getFaction())) {
            log.warn("disbandArmy: Player [{}] and army [{}] do not share the same faction ([{}] and [{}])", player, army, player.getFaction(), army.getFaction());
            throw ArmyServiceException.notAllowedToDisbandNotSameFaction(army.getArmyType(), army.getName(), army.getFaction().getName());
        }

        Faction faction = player.getFaction();

        boolean isAllowed = forced; //if it's a forced delete, instantly get permission

        log.debug("Checking if executor is faction leader");
        if(player.equals(faction.getLeader())) {
            log.debug("Player is faction leader - allowed to disband army");
            isAllowed = true;
        }

        //TODO: Check for lord as well

        if(!isAllowed) {
            log.warn("disbandArmy: Player [{}] is neither faction leader or lord of [{}] and therefore cannot disband army [{}]!", player, faction, army);
            throw ArmyServiceException.notAllowedToDisband(army.getArmyType());
        }

        if(army.getBoundTo() != null) {
            army.getBoundTo().getRpChar().setBoundTo(null);
            army.setBoundTo(null);
        }

        log.debug("Deleting army [{}]", army);
        secureDelete(army, armyRepository);
        /*
        The following line was added to load the army.sieges list. If we don't do this, Jackson for some fucking reason
        cannot serialize the object. For some other fucking reason there is NOOOO post on the internet about this problem.
        Like wtf man
        TODO find better solution
         */
        var sieges = army.getSieges().size();
        log.info("Disbanded army [{}] - executed by player [{}]", army, player);
        return army;
    }

    @Transactional(readOnly = false)
    public Army setFreeArmyTokens(UpdateArmyDto dto) {
        log.debug("Trying to update the free tokens of army [{}] to value [{}]", dto.armyName(), dto.freeTokens());

        log.trace("Validating data");
        ServiceUtils.checkNulls(dto, List.of("freeTokens", "armyName"));
        ServiceUtils.checkBlanks(dto, List.of("armyName"));

        log.trace("Getting the army by name [{}]", dto.armyName());
        Army army = getArmyByName(dto.armyName());
        double oldTokens = army.getFreeTokens();

        if(dto.freeTokens() < 0) {
            log.warn("Tried to set tokens of army [{}] to [{}] - value has to be positive", army, dto.freeTokens());
            throw ArmyServiceException.tokenNegative(dto.freeTokens());
        }

        if(dto.freeTokens() > 30) {
            log.warn("Tried to set tokens of army [{}] to [{}] - value has to be max. 30", army, dto.freeTokens());
            throw ArmyServiceException.tokenAbove30(dto.freeTokens());
        }

        log.debug("Setting the tokens of the army to [{}]", dto.freeTokens());
        army.setFreeTokens(dto.freeTokens());

        log.debug("Persisting army");
        army = armyRepository.save(army);

        log.info("Set tokens of army [{}] from [{}] to [{}]", army, oldTokens, army.getFreeTokens());
        return army;
    }

    @Transactional(readOnly = false)
    public Army pickSiege(PickSiegeDto dto) {
        log.debug("Trying to pick siege [{}] for army [{}] from cb [{}] - executed by player [{}]", dto.siege(), dto.armyName(), dto.claimbuildName(), dto.executorDiscordId());

        log.trace("Validating data");
        ServiceUtils.checkAllNulls(dto);
        ServiceUtils.checkAllBlanks(dto);

        log.trace("Getting army by name");
        Army army = getArmyByName(dto.armyName());

        log.debug("Checking if army is an army and not a company");
        if(!army.getArmyType().equals(ArmyType.ARMY)) {
            log.warn("Tried to pick siege for [{}], which is a [{}]!", army.getName(), army.getArmyType().name());
            throw ArmyServiceException.siegeOnlyArmyCanPick(army.getName());
        }

        log.trace("Getting player instance");
        Player player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        boolean isAllowed = ServiceUtils.boundLordLeaderPermission(player, army);

        if(!isAllowed) {
            log.warn("Player is not bound to army and is not faction leader/lord of [{}]!", army.getFaction());
            throw ArmyServiceException.siegeNotFactionLeaderOrLord(army.getFaction().getName(), army.getName());
        }

        log.debug("Fetching claimbuild [{}]", dto.claimbuildName());
        Optional<ClaimBuild> foundCb = secureFind(dto.claimbuildName(), claimBuildRepository::findById);
        if(foundCb.isEmpty()) {
            log.warn("Found no claimbuild with name [{}]", dto.claimbuildName());
            throw ClaimBuildServiceException.noCbWithName(dto.claimbuildName());
        }

        ClaimBuild cb = foundCb.get();

        log.debug("Checking if army is in the same region as cb");
        if(!army.getCurrentRegion().equals(cb.getRegion())) {
            log.warn("Army [{}] is not in the same region as cb [{}] (Army's region: [{}], CB's region: [{}])", army.getName(), cb.getName(), army.getCurrentRegion().getId(), cb.getRegion().getId());
            throw ArmyServiceException.siegeArmyNotInSameRegionAsCB(army.getName(), army.getCurrentRegion().getId(), cb.getName(), cb.getRegion().getId());
        }

        log.debug("Checking if cb is owned by player's faction or an allied faction");
        if(!player.getFaction().equals(cb.getOwnedBy()) && !player.getFaction().getAllies().contains(cb.getOwnedBy())) {
            log.warn("CB [{}] is owned by Faction [{}] which is not allied to player [{}'s] Faction [{}]", cb.getName(), cb.getOwnedBy(), player.getIgn(), player.getFaction());
            throw ClaimBuildServiceException.differentFactionNotAllied(cb.getName(), cb.getOwnedBy().getName());
        }

        log.debug("Checking if inputted siege is available in CB");
        String inputtedSiege = dto.siege().toLowerCase();
        String availableSiege = cb.getSiege().toLowerCase();

        if(!availableSiege.contains(inputtedSiege)) {
            log.warn("Inputted siege [{}] is not available in cb's sieges [{}]", inputtedSiege, availableSiege);
            throw ArmyServiceException.siegeNotAvailable(dto.siege(), cb.getName(), cb.getSiege());
        }

        log.debug("Adding inputted siege to army siege");
        army.getSieges().add(dto.siege());

        log.trace("Persisting army");
        army = secureSave(army, armyRepository);

        log.info("Picked up siege [{}] for army [{}]!", dto.siege(), army);
        return army;
    }

    public UpkeepDto getUpkeepOfFaction(String factionName) {
        log.debug("Getting upkeep of faction [{}]", factionName);

        Objects.requireNonNull(factionName);

        log.debug("Fetching faction [{}] from database", factionName);
        Optional<Faction> fetchedFaction = secureFind(factionName, factionRepository::findFactionByName);

        if(fetchedFaction.isEmpty()) {
            log.warn("No faction found with name [{}] in database", factionName);
            throw FactionServiceException.noFactionWithNameFound(factionName);
        }

        log.debug("Calculating upkeep of faction [{}]", factionName);
        int armyCount = (int) fetchedFaction.get().getArmies().stream()
                .filter(army -> ArmyType.ARMY.equals((army.getArmyType())))
                .count();

        int upkeep = 1000 * armyCount;

        log.info("Upkeep Request - Faction: {}, army count: {}, upkeep: {}", factionName, armyCount, upkeep);
        return new UpkeepDto(fetchedFaction.get().getName(), armyCount, upkeep);
    }
    public List<UpkeepDto> upkeep() {

        log.debug("Fetching all Factions");
        List<Faction> factions = secureFind(factionRepository::findAll);

        log.trace("Instantiating upkeepDto List");
        List<UpkeepDto> upkeepDtoList = new ArrayList<>();

        log.debug("Iterating through factions:");
        factions.parallelStream().
                forEach(faction -> {
                    int armyCount = (int) faction.getArmies().stream()
                            .filter(army -> ArmyType.ARMY.equals(army.getArmyType()))
                            .count();
                    int upkeep = armyCount * 1000;
                    log.debug("Adding: Faction [{}], Army Count: [{}], Upkeep [{}]", faction.getName(), armyCount, upkeep);
                    upkeepDtoList.add(new UpkeepDto(faction.getName(), armyCount, upkeep));
                });

        return upkeepDtoList;
    }
    @Transactional(readOnly = false)
    public Army setIsPaid(UpdateArmyDto dto) {
        log.debug("Trying to set isPaid to true for army or company [{}]", dto);

        ServiceUtils.checkNulls(dto, List.of("armyName", "isPaid"));
        ServiceUtils.checkBlanks(dto, List.of("armyName"));

        String name = dto.armyName();

        log.debug("Fetching army or company with name [{}]", name);
        Army army = getArmyByName(name);

        log.debug("Setting is paid to [{}] for [{}]",dto.isPaid(), army.getName());
        army.setIsPaid(dto.isPaid());

        log.debug("Persisting [{}], isPaid [{}]", army.getName(), army.getIsPaid());
        army = secureSave(army, armyRepository);

        log.info("Successfully set isPaid to [{}]!", army.getIsPaid());
        return army;
    }

    /***
     * Gets the 10 oldest created armies that are not paid for
     * @return List of 10 armies, sorted by creation date
     */
    public List<Army> getUnpaid() {
        log.debug("Trying to get the 10 oldest unpaid armies or trading companies");

        log.trace("Fetching all armies and sorting list");
        List<Army> armies = secureFind(armyRepository::findAll).stream()
                .filter(army -> !army.getIsPaid())
                .sorted(Comparator.comparing(Army::getCreatedAt))
                .limit(10)
                .collect(Collectors.toList());

        log.info("Successfully returning list of 10 oldest armies or companies [{}]", armies);
        return armies;
    }
    public Army getArmyByName(String armyName) {
        log.debug("Getting army by name [{}]", armyName);
        log.trace("Checking for null");
        Objects.requireNonNull(armyName);

        log.trace("Fetching the army by name");
        Optional<Army> fetchedArmy = secureFind(armyName, armyRepository::findArmyByName);

        log.trace("Checking if the army exists");
        if(fetchedArmy.isEmpty()) {
            log.warn("No army with the name [{}] found!",armyName);
            throw ArmyServiceException.noArmyWithName("Army or Company", armyName);
        }
        Army army = fetchedArmy.get();
        log.debug("Found army [{}] - type: [{}]", army.getName(), army.getArmyType().name());
        return army;
    }

    public UnitTypeDto[] convertUnitInputIntoUnits(String unitString) {
        log.debug("Converting unitString into units: [{}]", unitString);

        unitString = unitString
                .stripLeading()
                .stripTrailing();

        validateUnitString(unitString);

        // Unit:5-Gondor Unit:2
        List<UnitTypeDto> units = new ArrayList<>();
        String[] unitNameAmountSplit = unitString.split("-");

        for (String unitNameAmount : unitNameAmountSplit) {
            log.trace("Splitting [{}]", unitNameAmount);
            String[] unit = unitNameAmount.split(":");
            log.trace("Length of split: [{}]", unit.length);

            String unitName = unit[0];
            int amount = Integer.parseInt(unit[1]);

            units.add(new UnitTypeDto(unitName, amount));
        }

        return units.toArray(new UnitTypeDto[0]);
    }

    public void validateUnitString(String unitString) {
        log.debug("Validating unitString [{}]", unitString);
        // The two defining syntax chars in the unitString
        char[] syntaxChars = {':', '-'};

        // Expected Syntax char, first one is :, then alternating between - and :
        char expectedChar = ':';

        // Is also true at the start, from then every time a expectedChar is switched
        boolean firstCharAfterExpected = true;
        boolean possibleEnd = false;

        log.trace("Starting validation, unitString length: [{}]", unitString.length());

        for(int i = 0; i < unitString.length(); i++) {
            log.trace("Index: [{}]", i);
            log.trace("Expected next syntax char [{}]", expectedChar);
            char currentChar = unitString.charAt(i);
            log.trace("Current char: [{}]", currentChar);

            if(expectedChar == ':') {
                log.trace(": bracket");
                if(currentChar == expectedChar && !firstCharAfterExpected) {
                    log.trace("Current char is same as : [{}] and not first char after expected", currentChar);
                    expectedChar = '-';
                    firstCharAfterExpected = true;
                }
                else if (Character.isLetter(currentChar) || Character.isSpaceChar(currentChar)) {
                    log.trace("Char [{}] is letter or space, going into next iteration", currentChar);
                    firstCharAfterExpected = false;
                }
                else {
                    log.warn("Char [{}] at [{}] has created an error in unitString [{}], next expected [{}]", currentChar, i, unitString, expectedChar);
                    throw ArmyServiceException.invalidUnitString(unitString);
                }
            }
            else {
                log.trace("- bracket");
                if (Character.isDigit(currentChar)) {
                    log.trace("Char [{}] is a digit, possible end after this", currentChar);
                    firstCharAfterExpected = false;
                    possibleEnd = true;
                }
                else if (currentChar == expectedChar && !firstCharAfterExpected) {
                    log.trace("Current char is same as - [{}] and not first char after expected", currentChar);
                    expectedChar = ':';
                    firstCharAfterExpected = true;
                    possibleEnd = false;
                }
                else {
                    log.warn("Char [{}] at [{}] has created an error in unitString [{}], next expected [{}]", currentChar, i, unitString, expectedChar);
                    throw ArmyServiceException.invalidUnitString(unitString);
                }
            }

            if((i + 1) == unitString.length() && !possibleEnd) {
                log.warn("UnitSring is at end but not valid end");
                throw ArmyServiceException.invalidUnitString(unitString);
            }
        }
    }
}
