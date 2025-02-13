package com.ardaslegends.service.time;

import com.ardaslegends.domain.*;
import com.ardaslegends.repository.ArmyRepository;
import com.ardaslegends.repository.MovementRepository;
import com.ardaslegends.repository.player.PlayerRepository;
import com.ardaslegends.service.ArmyService;
import com.ardaslegends.service.MovementService;
import com.ardaslegends.service.PlayerService;
import com.ardaslegends.service.exceptions.logic.player.PlayerServiceException;
import com.ardaslegends.service.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

@RequiredArgsConstructor

@Slf4j
@Service
public class ScheduleService {

    private final MovementRepository movementRepository;
    private final ArmyRepository armyRepository;
    private final PlayerRepository playerRepository;
    private final MovementService movementService;
    private final ArmyService armyService;
    private final PlayerService playerService;
    private final TimeFreezeService timeFreezeService;
    private final Clock clock;

    @Scheduled(cron = "0 */15 * ? * *")
    @Transactional(readOnly = false)
    public void handleMovements() {
        OffsetDateTime startDateTime = OffsetDateTime.now(clock);
        long startNanos = System.nanoTime();
        log.info("Starting scheduled handling of movement - System time: [{}]", startDateTime);

        log.debug("Getting all active movements");
        List<Movement> allActiveMoves = movementRepository.findMovementsByIsCurrentlyActive(true);
        log.debug("Found [{}] active movements - continuing with handling", allActiveMoves.size());

        log.debug("Calling parallelStream handleSingleMovement");
        allActiveMoves.stream().forEach(movement -> handleSingleMovement(movement, startDateTime));

        log.debug("Saving all movements");
        allActiveMoves = movementService.saveMovements(allActiveMoves);

        long endNanos = System.nanoTime();
        BigDecimal neededTime = BigDecimal.valueOf((double) MILLISECONDS.convert(endNanos - startNanos, NANOSECONDS) / 1000);
        log.debug("Needed time in nanos: [{}]", endNanos - startNanos);
        log.info("Finished handling movements. Updated movements: [{}] - finished in {} seconds", allActiveMoves.size(), neededTime.toPlainString());
    }

    @Scheduled(cron = "0 */15 * ? * *")
    @Transactional(readOnly = false)
    public void handleHealings() {
        OffsetDateTime startDateTime = OffsetDateTime.now(clock);
        long startNanos = System.nanoTime();
        log.info("Starting scheduled handling of healings - System time: [{}]", startDateTime);

        log.debug("Getting all armies that are healing");
        List<Army> healingArmies = armyRepository.findArmyByIsHealingTrue();
        log.debug("Found [{}] healing armies - continuing with handling", healingArmies.size());

        log.debug("Getting all characters that are healing");
        List<Player> healingPlayers = playerRepository.queryPlayersWithHealingRpchars();
        log.debug("Found [{}] healing chars - continuing with handling", healingPlayers.size());

        log.debug("Calling parallelStream handleHealingArmy");
        healingArmies.stream().forEach(army -> handleHealingArmy(army, startDateTime));

        log.debug("Calling parallelStream handleHealingPlayer");
        healingPlayers.stream().forEach(player -> handleHealingPlayer(player, startDateTime));

        log.trace("Persisting data");
        healingArmies = armyService.saveArmies(healingArmies);
        healingPlayers = playerService.savePlayers(healingPlayers);

        long endNanos = System.nanoTime();
        BigDecimal neededTime = BigDecimal.valueOf((double) MILLISECONDS.convert(endNanos - startNanos, NANOSECONDS) / 1000);
        log.debug("Needed time in nanos: [{}]", endNanos - startNanos);
        log.info("Finished handling healing. Updated armies: [{}], updated chars: [{}] - finished in {} seconds", healingArmies.size(), healingPlayers.size(), neededTime.toPlainString());
    }

    private void handleSingleMovement(Movement movement, OffsetDateTime now) {
        log.debug("Handling movement of {} {} with path {}", movement.getMovingEntity(), movement.getMovingEntityName(),
                ServiceUtils.buildPathStringWithCurrentRegion(movement.getPath(), movement.getCurrentRegion()));
        log.debug("Movement data: {}", movement);

        if(timeFreezeService.isTimeFrozen()) {
            log.debug("Time is frozen - delaying movement");
            val timeSinceLastUpdate = Duration.between(movement.getLastUpdatedAt(), now);
            log.trace("Duration since last movement update: [{}]", ServiceUtils.formatDuration(timeSinceLastUpdate));
            log.debug("Delaying movement by [{}]", ServiceUtils.formatDuration(timeSinceLastUpdate));
            log.debug("Old ReachesNextRegionAt: [{}]", movement.getReachesNextRegionAt());
            movement.setReachesNextRegionAt(movement.getReachesNextRegionAt().plus(timeSinceLastUpdate));
            log.debug("New ReachesNextRegionAt: [{}]", movement.getReachesNextRegionAt());
            log.debug("Old EndsAt: [{}]", movement.getEndTime());
            movement.setEndTime(movement.getEndTime().plus(timeSinceLastUpdate));
            log.debug("New EndsAt: [{}]", movement.getEndTime());
        }

        log.trace("Entering loop while now [{}] is after reachesNextRegionAt [{}]", now, movement.getReachesNextRegionAt());
        while(now.isAfter(movement.getReachesNextRegionAt())) {
            log.trace("Now [{}] is after reachesNextRegionAt [{}]", now, movement.getReachesNextRegionAt());
            log.trace("Updating current region from [{}] to [{}]", movement.getCurrentRegion(), movement.getNextRegion());
            movement.setCurrentRegion(movement.getNextRegion());

            log.trace("Checking if current region is destination");
            if(movement.getNextPathElement() == null) {
                log.info("Movement of {} {} with path [{}] reached its destination, setting isActive to false", movement.getMovingEntity(), movement.getMovingEntityName(),
                        ServiceUtils.buildPathString(movement.getPath()));
                movement.end();
                break;
            }

            log.trace("Calculating new reachesNextRegionAt");
            val reachesNewRegionAt = movement.getReachesNextRegionAt().plusHours(movement.getNextPathElement().getActualCost());
            log.trace("Reaches new next region at: [{}]", reachesNewRegionAt);
            log.trace("Updating reachesNextRegionAt");
            movement.setReachesNextRegionAt(reachesNewRegionAt);
        }
        log.trace("Exited while loop");

        log.trace("Setting movement lastUpdatedAt to now [{}]", now);
        movement.setLastUpdatedAt(now);

        log.debug("Finished handling movement of {} {} with path {}", movement.getMovingEntity(), movement.getMovingEntityName(),
                ServiceUtils.buildPathStringWithCurrentRegion(movement.getPath(), movement.getCurrentRegion()));
    }

    private void handleHealingArmy(Army army, OffsetDateTime now) {
        log.debug("Handling healing army [{}]", army);
        OffsetDateTime endTime = army.getHealEnd();

        if(timeFreezeService.isTimeFrozen()) {
            log.debug("Time is frozen - delaying army healing");
            val timeSinceLastUpdate = Duration.between(army.getHealLastUpdatedAt(), now);
            log.trace("Duration since last army healing update: [{}]", ServiceUtils.formatDuration(timeSinceLastUpdate));
            log.debug("Delaying army healing by [{}]", ServiceUtils.formatDuration(timeSinceLastUpdate));
            log.debug("Old HealEnd: [{}]", army.getHealEnd());
            army.setHealEnd(army.getHealEnd().plus(timeSinceLastUpdate));
            log.debug("New HealEnd: [{}]", army.getHealEnd());
        }

        log.debug("Setting HealingLastUpdatedAt to now [{}]", now);
        army.setHealLastUpdatedAt(now);

        log.debug("Getting the hours between end date [{}] and current time [{}]", endTime, now);

        /*
        Get difference between now and endTime in hours
        This shows us how many hours are left of healing process
         */

        //we have to add 1 here because we want to know how many hours have passed
        //HOURS.between returns 0 if you have 00:59:59 minutes/seconds
        int hoursLeft = (int) HOURS.between(now, endTime) + 1;
        if(now.isAfter(endTime)) {
            log.debug("Current date is after end date, setting hours left to 0");
            hoursLeft = 0;
        }
        log.debug("Hours left: [{}]", hoursLeft);

        if(hoursLeft <= 0) {
            log.debug("Healing has less than or 0 hours left - completing it");
            log.debug("Healing all the units");
            army.getUnits().stream().forEach(unit -> unit.setAmountAlive(unit.getCount()));
            log.info("Army [{}] has finished its healing process!", army.getName());
            army.resetHealingStats();
        }

        /*
        We get the hours healed since last time by subtracting the current hours left
        with the last hours left (the value that was last stored in army)
         */

        int hoursHealedSinceLastTime = army.getHoursLeftHealing() - hoursLeft;
        log.debug("Hours healed since last time: [{}]", hoursHealedSinceLastTime);

        //If we didn't heal an hour since last time, exit function

        if(hoursHealedSinceLastTime == 0 && hoursLeft != 0) {
            log.debug("No hour has passed for this healing - exiting function");
            return;
        }

        /*
        Sets the divisor to 24
        If stationed at a stronghold, set it to half the amount (x2 heal speed)
        This is explained in the later comment
         */

        int divisor = 24;
        log.trace("Setting divisor to [{}]", divisor);
        if(army.getStationedAt().getType().equals(ClaimBuildType.STRONGHOLD)) {
            divisor /= 2;
            log.trace("Army is stationed at Stronghold, setting divisor to [{}]", divisor);
        }

        //The amount of hours the army has healed after the last replenish (excluding hoursHealedSinceLastTime)
        int hoursSinceLastReplenishWithoutCurrent = army.getHoursHealed() % divisor;
        //The amount of hours the army has healed after the last replenish (including hoursHealedSinceLastTime)
        int hoursHealedSinceLastReplenish = hoursSinceLastReplenishWithoutCurrent + hoursHealedSinceLastTime;

        /*
        We enter a loop that lasts as long as we hoursHealedSinceLastReplenish > divisor
        We replenish the troops in every iteration and subtract hoursHealedSinceLastReplenish - divisor
        For example 48 - 24 = 24 - 24 = 0 -> replenish alive units 2 times
        When army stationed claimbuild is a STRONGHOLD, then subtract by 12 (replenish every 12h)
         */

        log.trace("Entering loop");
        while(hoursHealedSinceLastReplenish >= divisor) {
            log.trace("hoursHealedSinceLastReplenish: [{}]", hoursHealedSinceLastReplenish);
            log.trace("Subtracting divisor [{}] from hoursHealedSinceLastReplenish [{}]", divisor, hoursHealedSinceLastReplenish);
            hoursHealedSinceLastReplenish -= divisor;
            log.trace("New hoursHealedSinceLastReplenish: [{}]", hoursHealedSinceLastReplenish);

            //only get units that are not fully replenished sorted by token cost ascending
            List<Unit> units = army.getUnits().stream()
                    .filter(unit -> unit.getAmountAlive() < unit.getCount())
                    .sorted(Comparator.comparing(Unit::getCost))
                    .toList();

            double replenishTokens = 6.0;
            int currentUnitIndex = 0;
            Unit currentUnit = units.get(currentUnitIndex);

            while(replenishTokens > 0) {
                log.trace("Starting to replenish unit: [{}]", currentUnit);
                log.trace("Unit [{}] has currently {}/{} alive units", currentUnit.getUnitType(), currentUnit.getAmountAlive(), currentUnit.getCount());
                log.trace("Replenish tokens left: [{}]", replenishTokens);

                int deadUnits = currentUnit.getCount() - currentUnit.getAmountAlive();
                log.trace("Dead units: [{}]", deadUnits);
                double tokensToHeal = deadUnits * currentUnit.getCost();
                log.trace("Tokens to heal: [{}]", tokensToHeal);
                if(tokensToHeal >= replenishTokens) {
                    log.trace("More tokens to heal than replenish tokens available - using all replenish tokens on unit [{}]", currentUnit);
                    int canHealUnits = (int) (replenishTokens / currentUnit.getCost());
                    log.trace("Unit has cost [{}] - can heal [{}] with [{}] refresh tokens", currentUnit.getCost(), canHealUnits, replenishTokens);
                    currentUnit.setAmountAlive(currentUnit.getAmountAlive() + canHealUnits);
                    log.trace("Unit now has {}/{} units", currentUnit.getAmountAlive(), currentUnit.getCount());
                    replenishTokens = 0;
                    log.trace("Set replenish tokens to [{}]", replenishTokens);
                }
                else {
                    log.trace("Less units to heal than replenish tokens available - healing unit [{}] to full", currentUnit.getUnitType());
                    currentUnit.setAmountAlive(currentUnit.getCount());
                    log.trace("Unit now has {}/{} units", currentUnit.getAmountAlive(), currentUnit.getCount());
                    replenishTokens -= tokensToHeal;
                    log.trace("Set replenish tokens to [{}]", replenishTokens);

                    log.trace("Setting the next unit");
                    if(currentUnitIndex == units.size()-1) {
                        log.trace("current unit was last in list, not setting new current unit");
                        log.info("Army [{}] has finished its healing process!", army.getName());
                        army.resetHealingStats();
                        replenishTokens = 0;
                    }
                    else {
                        currentUnitIndex++;
                        currentUnit = units.get(currentUnitIndex);
                        log.trace("Set new current unit to [{}]", currentUnit);
                    }
                }
            }
            log.trace("Exited loop");

            if(!army.allUnitsAlive()) {
                log.trace("Army not fully healed - updating parameters");
                log.trace("Setting the new amount of hours healed to [{}]", army.getHoursHealed() + hoursHealedSinceLastTime);
                army.setHoursHealed(army.getHoursHealed() + hoursHealedSinceLastTime);
                log.trace("Setting new hours left to [{}]", hoursLeft);
                army.setHoursLeftHealing(hoursLeft);
            }

            log.debug("Finished army [{}]", army);
        }
    }

    private void handleHealingPlayer(Player player, OffsetDateTime now) {
        log.debug("Handling healing player [{}]", player);
        RPChar rpChar = player.getActiveCharacter().orElseThrow(PlayerServiceException::noRpChar);
        log.trace("Got player's rpchar [{}]", rpChar);
        OffsetDateTime endTime = rpChar.getHealEnds();

        if(timeFreezeService.isTimeFrozen()) {
            log.debug("Time is frozen - delaying army healing");
            val timeSinceLastUpdate = Duration.between(rpChar.getHealLastUpdatedAt(), now);
            log.trace("Duration since last rpChar healing update: [{}]", ServiceUtils.formatDuration(timeSinceLastUpdate));
            log.debug("Delaying rpChar healing by [{}]", ServiceUtils.formatDuration(timeSinceLastUpdate));
            log.debug("Old HealEnd: [{}]", rpChar.getHealEnds());
            rpChar.setHealEnds(rpChar.getHealEnds().plus(timeSinceLastUpdate));
            log.debug("New HealEnd: [{}]", rpChar.getHealEnds());
        }

        log.debug("Setting HealingLastUpdatedAt to now [{}]", now);
        rpChar.setHealLastUpdatedAt(now);

        log.debug("Getting the hours between end date [{}] and current time [{}]", endTime, now);

        /*
        Get difference between now and endTime in hours
        This shows us how many hours are left of healing process
         */

        //we have to add 1 here because we want to know how many hours have passed
        //HOURS.between returns 0 if you have 00:59:59 minutes/seconds
        int hoursLeft = (int) HOURS.between(now, endTime) + 1;
        if(now.isAfter(endTime)) {
            log.debug("Current date is after end date, setting hours left to 0");
            hoursLeft = 0;
        }
        log.debug("Hours left: [{}]", hoursLeft);

        if(hoursLeft <= 0) {
            log.info("Character [{}] of player [{}] finished healing - setting isInjured and isHealing to false", rpChar, player);
            rpChar.setInjured(false);
            rpChar.setIsHealing(false);
            log.trace("Exiting function");
            return;
        }
        log.debug("Character [{}] of player [{}] still has [{}] hours left for healing and therefore has not finished yet!", rpChar, player, hoursLeft);

    }

}
