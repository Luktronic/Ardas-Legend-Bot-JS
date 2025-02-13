package com.ardaslegends.service;

import com.ardaslegends.domain.*;
import com.ardaslegends.repository.ArmyRepository;
import com.ardaslegends.repository.MovementRepository;
import com.ardaslegends.repository.player.PlayerRepository;
import com.ardaslegends.repository.region.RegionRepository;
import com.ardaslegends.service.dto.army.MoveArmyDto;
import com.ardaslegends.service.dto.player.DiscordIdDto;
import com.ardaslegends.service.dto.player.rpchar.MoveRpCharDto;
import com.ardaslegends.service.exceptions.logic.player.PlayerServiceException;
import com.ardaslegends.service.exceptions.ServiceException;
import com.ardaslegends.service.exceptions.logic.army.ArmyServiceException;
import com.ardaslegends.service.exceptions.logic.movement.MovementServiceException;
import com.ardaslegends.service.utils.ServiceUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor

@Slf4j
@Service
@Transactional(readOnly = true)
public class MovementService extends AbstractService<Movement, MovementRepository>{

    private final MovementRepository movementRepository;
    private final RegionRepository regionRepository;
    private final ArmyRepository armyRepository;
    private final ArmyService armyService;
    private final PlayerRepository playerRepository;
    private final PlayerService playerService;
    private final Pathfinder pathfinder;
    private final RpCharService rpCharService;

    // TODO: Check if time is frozen -> if yes, cancel request
    // TODO: Check if army is in a battle -> if yes, cancel request
    // TODO: Check if army is healing -> if yes, ask to stop healing
    @Transactional(readOnly = false)
    public Movement createArmyMovement(MoveArmyDto dto) {
        log.debug("Trying to move Army [{}] executed by [{}] to Region [{}]", dto.armyName(), dto.executorDiscordId(), dto.toRegion());

        Movement movement = calculateArmyMovement(dto);

        log.debug("Saving Movement to database");
        secureSave(movement, movementRepository);

        log.info("Successfully saved movement [{}]", movement);
        return movement;
    }

    public Movement calculateArmyMovement(MoveArmyDto dto) {
        ServiceUtils.checkAllNulls(dto);
        ServiceUtils.checkAllBlanks(dto);

        log.debug("Fetching required data");

        log.trace("Fetching player");
        Player player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        log.trace("Fetching army entity");
        Army army = armyService.getArmyByName(dto.armyName());

        log.trace("Fetching region entity");
        Optional<Region> fetchedRegion = secureFind(dto.toRegion(), regionRepository::findById);

        if(fetchedRegion.isEmpty()) {
            log.warn("Desired region [{}] does not exist in the database", dto.toRegion());
            throw ServiceException.regionDoesNotExist(dto.toRegion());
        }
        Region region = fetchedRegion.get();

        log.debug("Checking if army is already in the desired region");
        if(dto.toRegion().equals(army.getCurrentRegion().getId())) {
            log.warn("Army is already in desired region [{}], no movement required", dto.toRegion());
            throw ArmyServiceException.cannotMoveArmyAlreadyInRegion(army.getArmyType(),army.toString(), dto.toRegion());
        }

        log.debug("Checking if army is currently performing a movement");
        if(secureFind(army, movementRepository::findMovementByArmyAndIsCurrentlyActiveTrue).isPresent()) {
            log.warn("Army [{}] is currently performing a movement", dto.armyName());
            throw ArmyServiceException.cannotMoveArmyDueToArmyBeingInMovement(army.getArmyType(),army.getName());
        }

        log.debug("Checking if army is older than 24h");
        if(army.isYoungerThan24h()) {
            log.warn("Army [{}] is younger than 24h and therefore cannot move!", army);
            long hoursUntilMove = 24 - Duration.between(army.getCreatedAt(), OffsetDateTime.now()).toHours();
            log.debug("Army can move again in [{}] hours", hoursUntilMove);
            throw ArmyServiceException.cannotMoveArmyWasCreatedRecently(army.getName(), hoursUntilMove);
        }

        log.debug("Checking if executor is allowed to perform the movement");
        boolean isAllowed = ServiceUtils.boundLordLeaderPermission(player, army);

        if(!isAllowed) {
            log.warn("Player [{}] in Faction [{}] does not have permission to move armies", player.getIgn(), player.getFaction());
            throw ArmyServiceException.noPermissionToPerformThisAction();
        }

        log.debug("Player [{}] is allowed to move army [{}], executing pathfinder", player, army);
        List<PathElement> path = pathfinder.findShortestWay(army.getCurrentRegion(),region,player, false);

        log.debug("Removing movement cost from faction stockpile");
        army.getFaction().subtractFoodFromStockpile(ServiceUtils.getFoodCost(path));

        var currentTime = OffsetDateTime.now();
        log.debug("Creating movement object");
        int hoursUntilDone = ServiceUtils.getTotalPathCost(path);  //Gets a sum of all the
        val reachesNextRegionAt = currentTime.plusHours(path.get(1).getActualCost());
        val character = player.getActiveCharacter().orElseThrow(PlayerServiceException::noRpChar);
        Movement movement = new Movement(character, army, false, path, currentTime, currentTime.plusHours(hoursUntilDone), true, reachesNextRegionAt);
        return movement;
    }

    @Transactional(readOnly = false)
    public Movement cancelArmyMovement(MoveArmyDto dto) {
        log.debug("Trying to cancel movement of army [{}] (executed by player [{}])", dto.armyName(), dto.executorDiscordId());

        log.trace("Validating data");
        ServiceUtils.checkNulls(dto, List.of("armyName", "executorDiscordId"));
        ServiceUtils.checkBlanks(dto, List.of("armyName", "executorDiscordId"));

        log.trace("Getting the army instance");
        Army army = armyService.getArmyByName(dto.armyName());

        log.trace("Getting the player instance");
        Player player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        boolean isAllowed = ServiceUtils.boundLordLeaderPermission(player, army);

        if(!isAllowed) {
            log.warn("Player [{}] is not allowed to cancel movements of army [{}]", player, army);
            throw MovementServiceException.notAllowedToCancelMove();
        }

        log.trace("Getting active movement for army [{}]", army);
        Movement movement = getActiveMovementByArmy(army);

        log.debug("Setting the movement to inactive");
        movement.setIsCurrentlyActive(false);

        log.debug("Persisting the movement");
        movementRepository.save(movement);

        log.info("Cancelled movement of army [{}] - Start: [{}] ([{}]) - End: [{}] ([{}])", army, movement.getStartRegionId(), movement.getStartTime(), movement.getDestinationRegionId(), movement.getEndTime());
        return movement;
    }

    @Transactional(readOnly = false)
    public Movement createRpCharMovement(MoveRpCharDto dto) {
        log.debug("Moving RpChar of player {} to Region {}", dto.discordId(), dto.toRegion());

        var movement = calculateRpCharMovement(dto);

        log.trace("Saving the new movement");
        movement = secureSave(movement, movementRepository);

        log.info("Successfully created new Movement for the RPChar '{}'", movement.getRpChar().getName());
        return movement;
    }

    public Movement calculateRpCharMovement(MoveRpCharDto dto) {
        log.trace("Validating Data");
        ServiceUtils.checkAllNulls(dto);
        ServiceUtils.checkAllBlanks(dto);

        log.trace("Getting the player");
        Optional<Player> fetchedPlayer = secureFind(dto.discordId(), playerRepository::findByDiscordID);

        if(fetchedPlayer.isEmpty()) {
            log.warn("No player found with discordId [{}]", dto.discordId());
            throw new IllegalArgumentException("No player found with discordId [%s]".formatted(dto.discordId()));
        }
        Player player = fetchedPlayer.get();
        log.debug("Checking if the Player has a RP Char");
        log.trace("Setting the RPChar");
        RPChar rpChar = player.getActiveCharacter().orElseThrow(() -> {
            log.warn("Player {} has no RP Char!", player);
            return PlayerServiceException.noRpChar();
        });


        log.debug("Checking if destination is the current region");
        if(dto.toRegion().equals(rpChar.getCurrentRegion().getId())) {
            log.warn("Character is already in region {}!", dto.toRegion());
            throw ServiceException.cannotMoveRpCharAlreadyInRegion(rpChar, rpChar.getCurrentRegion());
        }

        log.debug("Checking if rpChar is bound to army");
        if(rpChar.getBoundTo() != null) {
            log.warn("RpChar is currently bound to army!");
            throw ServiceException.cannotMoveRpCharBoundToArmy(rpChar, rpChar.getBoundTo());
        }

        log.debug("Checking if rpchar is currently healing");
        if(rpChar.getIsHealing()) {
            log.warn("RpChar [{}] is currently healing and therefore cannot move", rpChar);
            throw MovementServiceException.cannotMoveCharIsHealing(rpChar.getName());
        }

        log.debug("Checking if rpChar is already in a movement");
        List<Movement> playerMovements = secureFind(rpChar, movementRepository::findMovementsByRpChar);
        if(playerMovements.stream().anyMatch(Movement::getIsCurrentlyActive)) { //Checking if there are any active movements
            log.warn("Player {} is already involved in a movement!", player);
            throw ServiceException.cannotMoveRpCharAlreadyMoving(rpChar);
        }

        //Setting up Region Data

        log.trace("Find the region the player is moving to");
        Optional<Region> fetchedToRegion = secureFind(dto.toRegion(), regionRepository::findById);

        if(fetchedToRegion.isEmpty()) {
            log.warn("User inputed to Region does not exist [{}]", dto.toRegion());
            throw new IllegalArgumentException("The region %s does not exist!".formatted(dto.toRegion()));
        }

        Region toRegion = fetchedToRegion.get();

        log.trace("Getting the RPChar's current region");
        Region fromRegion = rpChar.getCurrentRegion();

        log.debug("Calling the pathfinder to find the fastest way from '{}' -> '{}'", fromRegion.getId(), toRegion.getId());
        List<PathElement> path = pathfinder.findShortestWay(fromRegion, toRegion, player, true);

        log.trace("Getting the current time");
        OffsetDateTime currentTime = OffsetDateTime.now();

        log.trace("Building the movement object");
        int hoursUntilDone = ServiceUtils.getTotalPathCost(path);
        val reachesNextRegionAt = currentTime.plusHours(path.get(1).getActualCost());
        Movement movement = new Movement(rpChar, null, true, path, currentTime, currentTime.plusHours(hoursUntilDone), true, reachesNextRegionAt);

        return movement;
    }

    @Transactional(readOnly = false)
    public Movement cancelRpCharMovement(DiscordIdDto dto) {
        log.debug("Cancelling the rp char movement of player {}", dto.discordId());

        //validating data
        log.trace("Validating Data");
        ServiceUtils.checkAllNulls(dto);
        ServiceUtils.checkAllBlanks(dto);

        log.trace("Getting the player");
        Optional<Player> fetchedPlayer = secureFind(dto.discordId(), playerRepository::findByDiscordID);

        if(fetchedPlayer.isEmpty()) {
            log.warn("No player found with discordId [{}]", dto.discordId());
            throw new IllegalArgumentException("No player found with discordId [%s]".formatted(dto.discordId()));
        }
        Player player = fetchedPlayer.get();
        RPChar rpChar = player.getActiveCharacter().orElseThrow(() -> {
            log.warn("Player {} has no RP Char!", player);
            return PlayerServiceException.noRpChar();
        });

        log.trace("Searching for active movements of this player");
        Movement movement = getActiveMovementByChar(player);

        log.debug("Setting movement to inactive");
        movement.setIsCurrentlyActive(false);

        log.debug("Persisting movement");
        movement = secureSave(movement, movementRepository);

        return movement;
    }

    public Pair<Optional<Movement>, List<Movement>> getArmyMovements(@NonNull String armyName) {
        log.debug("Trying to get movements for army [{}]", armyName);

        log.trace("Fetching Army with name [{}]", armyName);
        val army = armyService.getArmyByName(armyName);

        log.trace("Fetching current movement for army [{}]", army.getName());
        val currentMovement = secureFind(army, movementRepository::findMovementByArmyAndIsCurrentlyActiveTrue);

        log.trace("Fetching past movements of army [{}]", army.getName());
        val pastMovements = secureFind(army, movementRepository::findMovementByArmyAndIsCurrentlyActiveFalse);

        return Pair.of(currentMovement, pastMovements);
    }

    public Pair<Optional<Movement>, List<Movement>> getCharMovements(@NonNull String charName) {
        log.debug("Trying to get movements for char [{}]", charName);

        log.trace("Fetching char with name [{}]", charName);
        val character = rpCharService.getRpCharByName(charName);

        log.trace("Fetching current movement for char [{}]", charName);
        val currentMovement = secureFind(character, movementRepository::findMovementByRpCharAndIsCurrentlyActiveTrue);

        log.trace("Fetching past movements of char [{}]", character.getName());
        val pastMovements = secureFind(character, movementRepository::findMovementByRpCharAndIsCurrentlyActiveFalse);

        return Pair.of(currentMovement, pastMovements);
    }

    public Movement getActiveMovementByArmy(Army army) {
        log.debug("Trying to get an active Movement for the army [{}]", army);

        log.trace("Executing the secureFind");
        Optional<Movement> fetchedMove = secureFind(army, movementRepository::findMovementByArmyAndIsCurrentlyActiveTrue);

        log.debug("Checking if a movement was found");
        if(fetchedMove.isEmpty()) {
            log.warn("No active movement was found for the army [{}]!", army);
            throw MovementServiceException.noActiveMovementArmy(army.getName());
        }

        Movement movement = fetchedMove.get();
        log.debug("Found a movement from region [{}] to [{}]!", movement.getStartRegionId(), movement.getDestinationRegionId());

        return movement;
    }

    public Movement getActiveMovementByChar(Player player) {
        log.debug("Trying to get an active Movement for the player [{}]", player);

        val character = player.getActiveCharacter().orElseThrow(() -> {
            log.warn("Player {} has no RP Char!", player);
            return PlayerServiceException.noRpChar();
        });

        log.trace("Executing the secureFind");
        Optional<Movement> fetchedMove = secureFind(character, movementRepository::findMovementByRpCharAndIsCurrentlyActiveTrue);

        log.debug("Checking if a movement was found");
        if(fetchedMove.isEmpty()) {
            log.warn("No active movement was found for the player [{}]!", player);
            throw MovementServiceException.noActiveMovementChar(character.getName());
        }

        Movement movement = fetchedMove.get();
        log.debug("Found a movement from region [{}] to [{}]!", movement.getStartRegionId(), movement.getDestinationRegionId());

        return movement;
    }

    public Movement saveMovement(Movement movement) {
        log.debug("Saving movement [{}]", movement);
        return secureSave(movement, movementRepository);
    }

    public List<Movement> saveMovements(List<Movement> movements) {
        log.debug("Saving movements [{}]", movements);
        return secureSaveAll(movements, movementRepository);
    }

}
