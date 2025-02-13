package com.ardaslegends.service.war;

import com.ardaslegends.domain.*;
import com.ardaslegends.domain.war.battle.*;
import com.ardaslegends.repository.war.WarRepository;
import com.ardaslegends.repository.war.QueryWarStatus;
import com.ardaslegends.repository.war.battle.BattleRepository;
import com.ardaslegends.service.*;
import com.ardaslegends.service.dto.war.battle.ConcludeBattleDto;
import com.ardaslegends.service.dto.war.battle.RpCharCasualtyDto;
import com.ardaslegends.service.dto.war.battle.SurvivingUnitsDto;
import com.ardaslegends.service.dto.war.battle.CreateBattleDto;
import com.ardaslegends.service.discord.DiscordService;
import com.ardaslegends.service.discord.messages.war.BattleMessages;
import com.ardaslegends.service.exceptions.logic.rpchar.RpCharServiceException;
import com.ardaslegends.service.exceptions.logic.war.BattleServiceException;
import com.ardaslegends.service.exceptions.logic.army.ArmyServiceException;
import com.ardaslegends.service.time.TimeFreezeService;
import com.ardaslegends.service.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class BattleService extends AbstractService<Battle, BattleRepository> {
    private final BattleRepository battleRepository;
    private final ArmyService armyService;
    private final PlayerService playerService;
    private final RpCharService rpCharService;
    private final ClaimBuildService claimBuildService;
    private final WarRepository warRepository;
    private final Pathfinder pathfinder;
    private final FactionService factionService;
    private final TimeFreezeService timeFreezeService;
    private final DiscordService discordService;

    @Transactional(readOnly = false)
    public Battle createBattle(CreateBattleDto createBattleDto) {
        log.debug("Creating battle with data {}", createBattleDto);
        Objects.requireNonNull(createBattleDto, "CreateBattleDto must not be null");
        Objects.requireNonNull(createBattleDto.battleName(), "BattleName must not be null");

        if (!createBattleDto.isFieldBattle())
            Objects.requireNonNull(createBattleDto.claimBuildName(), "Name of claim build attacked must not be null when creating a claim build battle");

        log.debug("Calling getPlayerByDiscordId with id: [{}]", createBattleDto.executorDiscordId());
        Player executorPlayer = playerService.getPlayerByDiscordId(createBattleDto.executorDiscordId());
        log.debug("Calling getArmyByName with name: [{}]", createBattleDto.attackingArmyName());
        Army attackingArmy = armyService.getArmyByName(createBattleDto.attackingArmyName());

        log.debug("Checking if army is older than 24h");
        if(attackingArmy.isYoungerThan24h()) {
            log.warn("Army [{}] cannot declare battle because it was created less than 24h ago!", attackingArmy.getName());
            throw BattleServiceException.armyYoungerThan24h(attackingArmy.getName());
        }

        log.debug("Checking if player has permission to start the battle");
        if (!ServiceUtils.boundLordLeaderPermission(executorPlayer, attackingArmy)) {
            log.warn("Player [{}] does not have the permission to start a battle with the army [{}]!", executorPlayer.getIgn(), createBattleDto.attackingArmyName());
            throw ArmyServiceException.noPermissionToPerformThisAction();
        }

        log.debug("Checking if army has a player bound to it");
        if(attackingArmy.getBoundTo() == null) {
            log.warn("Cannot declare battle because attacking army [{}] is not bound to a player", attackingArmy.getName());
            throw BattleServiceException.noPlayerBound(attackingArmy.getName());
        }

        log.debug("Checking if army has enough health to start the battle");
        if(attackingArmy.getFreeTokens() <=0 ){
            log.warn("The attacking army: [{}] can not start a battle, because they don`t have enough health!", attackingArmy);
            throw BattleServiceException.notEnoughHealth();
        }

        log.debug("Checking if attacking army is currently in a movement");
        if(attackingArmy.getActiveMovement().isPresent()){
            log.warn("Attacking army is currently moving, cannot declare battle!");
            throw BattleServiceException.attackingArmyHasAnotherMovement();
        }

        log.debug("Setting attacking faction to: [{}]", attackingArmy.getFaction().getName());
        Faction attackingFaction = attackingArmy.getFaction();
        Faction defendingFaction; //Not initializing yet, defending faction is evaluated differently for field battles
        Region battleRegion;
        ClaimBuild attackedClaimbuild = null;

        Set<Army> defendingArmies = new HashSet<>();
        List<PathElement> path;

        if(createBattleDto.isFieldBattle()){
            log.debug("Declared battle is field battle");
            log.debug("Fetching single defending army with name: [{}]", createBattleDto.defendingArmyName());
            log.trace("Calling getArmyByName with name: [{}]", createBattleDto.defendingArmyName());
            Army defendingArmy = armyService.getArmyByName(createBattleDto.defendingArmyName());

            Region defenderRegion = defendingArmy.getCurrentRegion();
            Region attackerRegion = attackingArmy.getCurrentRegion();

            log.debug("Checking if the defending army is in same region as attacking army for the next 24h");
            log.debug("Attacking army region: [{}]", attackerRegion);
            log.debug("Defending army region: [{}]", defenderRegion);

            if(!defenderRegion.equals(attackerRegion)) {
                log.warn("Attacking army is not in the same region [{}] as defending army [{}]", attackerRegion, defenderRegion);
                throw BattleServiceException.notInSameRegion(attackingArmy, defendingArmy);
            }

            log.debug("Checking if defending army is moving away");
            if(defendingArmy.getActiveMovement().isPresent()) {
                var activeMovement = defendingArmy.getActiveMovement().get();
                log.debug("Defending army [{}] is moving [{}]", defendingArmy, activeMovement);
                log.debug("Next region: [{}] - Duration until next region: [{}]", activeMovement.getNextRegion(), ServiceUtils.formatDuration(activeMovement.getDurationUntilNextRegion()));

                if(activeMovement.getDurationUntilNextRegion().minusHours(24).isNegative()) {
                    log.debug("Next region is reached in <= 24h");
                    log.warn("Cannot declare battle - defending army cannot be reached because it is moving away in [{}]!", ServiceUtils.formatDuration(activeMovement.getDurationUntilNextRegion()));
                    throw BattleServiceException.defendingArmyIsMovingAway(defendingArmy);
                }
                log.debug("Defending army is moving but is still in the region for the next 24h");

            }

            defendingArmies.add(defendingArmy);
            log.debug("Setting defending Faction to: [{}]", defendingArmy.getFaction().getName());
            defendingFaction = defendingArmy.getFaction();
            battleRegion = defendingArmy.getCurrentRegion();
        }
        else {
            log.debug("Declared battle is claimbuild battle");
            log.debug("Fetching all stationed armies at claimbuild: [{}]", createBattleDto.claimBuildName());
            log.trace("Calling getClaimBuildByName with name: [{}]", createBattleDto.claimBuildName());
            attackedClaimbuild = claimBuildService.getClaimBuildByName(createBattleDto.claimBuildName());
            defendingFaction = attackedClaimbuild.getOwnedBy();
            battleRegion = attackedClaimbuild.getRegion();
            List<Army> stationedArmies = attackedClaimbuild.getStationedArmies();

            log.debug("Checking if claimbuild [{}] is starter hamlet of faction [{}]", attackedClaimbuild.getName(), defendingFaction.getName());
            if(attackedClaimbuild.getName().toLowerCase().endsWith("starter hamlet")) {
                log.warn("Cannot declare battle on clamibuild [{}] because it is the starter hamlet of faction [{}]", attackedClaimbuild.getName(), defendingFaction.getName());
                throw BattleServiceException.cannotAttackStarterHamlet();
            }


            if(!attackingArmy.getCurrentRegion().equals(attackedClaimbuild.getRegion())) {
                log.debug("Army is not in region of claimbuild");
                log.debug("Calling pathfinder to find shortest way from regions [{}] to [{}]", attackingArmy.getCurrentRegion(), battleRegion);
                path = pathfinder.findShortestWay(attackingArmy.getCurrentRegion(), attackedClaimbuild.getRegion(),executorPlayer,true);
                log.debug("Path: [{}], duration: [{} days]", ServiceUtils.buildPathString(path), ServiceUtils.getTotalPathCost(path));

                log.debug("Checking if claimbuild is reachable in 24 hours");
                if(ServiceUtils.getTotalPathCost(path) > 24){
                    log.warn("Cannot declare battle - Claimbuild [{}] is not in 24 hour reach of attacking army [{}]", attackedClaimbuild.getName(), attackingArmy.getName());
                    throw BattleServiceException.battleNotAbleDueHours();
                }
            }
            else
                log.debug("Army [{}] is already in region [{}] of Claimbuild [{}]", attackingArmy.getName(), attackingArmy.getCurrentRegion().getId(), attackedClaimbuild.getName());


            if(stationedArmies.isEmpty())
                log.debug("No armies stationed at claim build with name: [{}]", createBattleDto.claimBuildName());
            else {
                log.debug("[{}] armies stationed at claim build with name: [{}]", stationedArmies.size(), createBattleDto.claimBuildName());
                defendingArmies.addAll(stationedArmies);
            }
        }

        val wars = warRepository.queryWarsBetweenFactions(attackingFaction, defendingFaction, QueryWarStatus.ACTIVE);
        if(wars.isEmpty()){
            log.warn("Cannot create battle: The attacking faction [{}] and the defending faction [{}] are not at war with each other", attackingFaction.getName(), defendingFaction.getName());
            throw BattleServiceException.factionsNotAtWar(attackingFaction.getName(), defendingFaction.getName());
        }

        log.debug("Creating BattleLocation");
        BattleLocation battleLocation = new BattleLocation(battleRegion, createBattleDto.isFieldBattle(), attackedClaimbuild);
        log.debug("Created BattleLocation [{}]", battleLocation);

        log.debug("War information: " + wars);

        log.trace("Assembling Battle Object");
        final Battle createdBattle = new Battle(wars,
                createBattleDto.battleName(),
                Set.of(attackingArmy),
                defendingArmies,
                OffsetDateTime.now(),
                null,
                null,
                null,
                battleLocation);

        log.debug("Calling start24hTimer()");
        val timer = timeFreezeService.start24hTimer(() -> startBattle(createdBattle));
        log.debug("Setting battle.timeFrozenFrom to end date of 24h timer");
        createdBattle.setTimeFrozenFrom(timer.finishesAt());

        Battle battle;
        log.debug("Trying to persist the battle object");
        try {
            battle = secureSave(createdBattle, battleRepository);
        }
        catch (Exception e) {
            log.warn("Cancelling 24h timer since there was an error while persisting battle [{}]", createdBattle);
            timer.future().cancel(true);
            throw e;
        }

        discordService.sendMessageToRpChannel(BattleMessages.declareBattle(battle, discordService));

        log.info("Successfully created battle [{}]!", battle.getName());
        return battle;
    }

    private Battle startBattle(Battle battle) {
        log.debug("Starting battle [{}]", battle);

        Objects.requireNonNull(battle, "battle in startBattle() must not be null!");

        log.debug("Setting BattlePhase to {}", BattlePhase.ONGOING);
        battle.setBattlePhase(BattlePhase.ONGOING);

        log.debug("Calling freezeTime()");
        timeFreezeService.freezeTime();

        //TODO: teleport all aiding armies to battle location

        return battle;
    }

    @Transactional(readOnly = false)
    public Battle concludeBattle(ConcludeBattleDto concludeBattleDto) {
        log.debug("Concluding battle with data {}", concludeBattleDto);
        Objects.requireNonNull(concludeBattleDto, "ConcludeBattleDto must not be null!");
        ServiceUtils.checkAllNulls(concludeBattleDto);
        Arrays.stream(concludeBattleDto.playersKilled())
                .forEach(discordIdDto -> {
                    ServiceUtils.checkAllNulls(discordIdDto);
                    ServiceUtils.checkAllBlanks(discordIdDto);
                });

        Arrays.stream(concludeBattleDto.survivingUnits())
                .forEach(unitsDto -> {
                    ServiceUtils.checkAllNulls(unitsDto);
                    ServiceUtils.checkAllBlanks(unitsDto);
                });

        log.debug("Finding battle with id [{}]", concludeBattleDto.battleId());
        val battle = battleRepository.queryByIdOrElseThrow(concludeBattleDto.battleId());

        if(battle.getBattleResult().isPresent()) {
            log.warn("Battle [{}] already has a result [{}]", battle.getId(), battle.getBattleResult());
            throw BattleServiceException.battleAlreadyConcluded();
        }

        log.debug("Trying to find winner faction [{}]", concludeBattleDto.winnerFaction());
        val winnerFaction = factionService.getFactionByName(concludeBattleDto.winnerFaction());
        log.debug("Found faction [{}]", winnerFaction);

        log.debug("Looking if winner faction [{}] is on attacking side", winnerFaction.getName());
        val isWinnerOnAttackerSide = battle.getAttackingArmies().stream()
                .map(Army::getFaction)
                .anyMatch(faction -> faction.equals(winnerFaction));
        log.debug("Winner is on attacking side: [{}]", isWinnerOnAttackerSide);

        if(!isWinnerOnAttackerSide) {
            log.debug("Looking if winner faction [{}] is on defending side", winnerFaction.getName());
            val isWinnerOnDefendingSide = battle.getDefendingArmies().stream()
                    .map(Army::getFaction)
                    .anyMatch(faction -> faction.equals(winnerFaction));
            log.debug("Winner is on defending side: [{}]", isWinnerOnDefendingSide);

            if(!isWinnerOnDefendingSide) {
                log.warn("Faction [{}] is not part of battle [{}]", winnerFaction.getName(), battle.getId());
                throw BattleServiceException.factionNotPartOfBattle(winnerFaction.getName(), battle.getId());
            }
        }

        log.debug("Getting the initial faction of the winner side");
        val winnerInitialFaction = isWinnerOnAttackerSide ? battle.getInitialAttacker().getFaction() : battle.getInitialDefender();
        log.debug("Initial faction of winner side is [{}]", winnerInitialFaction.getName());

        log.debug("Updating all army casualties");
        val unitCasualties = new HashSet<UnitCasualty>();
        Arrays.stream(concludeBattleDto.survivingUnits())
                .forEach(survivingUnitsDto -> {
                    val casualties = updateSurvivingUnitsFromDto(survivingUnitsDto, battle);
                    unitCasualties.addAll(casualties);
                });

        log.debug("Updating all units of armies not present in survivingUnitsDto");
        val armyNamesMentioned = Arrays.stream(concludeBattleDto.survivingUnits())
                .map(SurvivingUnitsDto::army).toList();
        val armiesMentioned = battle.getPartakingArmies().stream()
                .filter(army -> armyNamesMentioned.contains(army.getName())).toList();
        log.debug("Armies mentioned in unitCasualties: [{}]", StringUtils.join(armiesMentioned, ", "));
        val armiesNotMentioned = battle.getPartakingArmies().stream()
                .filter(army -> !armiesMentioned.contains(army))
                .toList();
        log.debug("Armies not mentioned in unitCasualties: [{}]", StringUtils.join(armiesNotMentioned, ", "));
        log.debug("Killing all units of armies: [{}]", StringUtils.join(armiesNotMentioned, ", "));
        armiesNotMentioned.forEach(army -> {
            unitCasualties.addAll(killAllUnitsOfArmy(army, battle));
        });

        log.debug("Updating all player casualties");
        val rpCharCasualties = updateKilledPlayers(concludeBattleDto.playersKilled());

        log.debug("Creating BattleResult");
        val battleResult = new BattleResult(winnerInitialFaction, unitCasualties, rpCharCasualties);
        log.debug("Created BattleResult [{}]", battleResult);

        log.debug("Adding result to battle [{}]", battle);
        battle.setBattleResult(battleResult);
        log.debug("Setting BattlePhase to [{}]", BattlePhase.CONCLUDED);
        battle.setBattlePhase(BattlePhase.CONCLUDED);
        val now = OffsetDateTime.now();
        log.debug("Setting timeFrozenUntil to now [{}]", now);
        battle.setTimeFrozenUntil(now);

        log.debug("Checking if claimbuild ownership needs to be changed");
        if(!battle.isFieldBattle()) {
            val claimbuild = battle.getBattleLocation().getClaimBuild();
            log.debug("Battle is claimbuild battle");
            log.debug("Checking if old owner [{}] is different from battle winner [{}]", claimbuild.getOwnedBy().getName(), battleResult.getWinner().getName());
            if(!claimbuild.getOwnedBy().equals(battleResult.getWinner())) {
                log.debug("Old owner is different than battle winner - setting new owner to [{}]", battleResult.getWinner().getName());
                claimBuildService.changeOwner(claimbuild, battleResult.getWinner());
            }

        }

        log.debug("Persisting data");
        val armies = unitCasualties.stream().map(UnitCasualty::getUnit)
                .map(Unit::getArmy).toList();
        val rpChars = rpCharCasualties.stream().map(RpCharCasualty::getRpChar).toList();

        log.debug("Saving armies");
        armyService.saveArmies(armies);

        val armiesToDisband = armies.stream().filter(army -> !army.hasUnitsLeft()).toList();
        log.debug("Disbanding all armies that have no units left [{}]", StringUtils.join(armiesToDisband, ", "));
        armiesToDisband.forEach(armyService::disband);

        log.debug("Saving chars");
        rpCharService.saveRpChars(rpChars);
        log.debug("Saving battle");
        val savedBattle = secureSave(battle, battleRepository);

        discordService.sendMessageToRpChannel(BattleMessages.concludeBattle(battle, discordService));

        log.debug("All entities saved - unfreezing time");
        timeFreezeService.unfreezeTime();

        return savedBattle;
    }

    /**
     * Updates the units of the army specified in the SurvivingUnitsDto to only include the surviving
     * units also specified in the dto
     * @param dto Dto which contains the army and its surviving units
     * @param battle The battle which the casualties happened in
     * @return A set of UnitCasualty objects of the units that died in the battle.
     */
    private Set<UnitCasualty> updateSurvivingUnitsFromDto(SurvivingUnitsDto dto, Battle battle) {
        log.debug("Updating the surviving units of army [{}]", dto.army());

        log.debug("Finding army [{}] in battle [{}]", dto.army(), battle);
        val foundArmy = battle.getPartakingArmies().stream()
                .filter(army -> army.getName().equals(dto.army()))
                .findFirst();

        if(foundArmy.isEmpty()) {
            log.warn("No army with name [{}] found in battle [{}]!", dto.army(), battle);
            throw BattleServiceException.armyNotPartOfBattle(dto.army(), battle.getId());
        }

        val army = foundArmy.get();
        log.debug("Found army [{}]", army);
        val units = army.getUnits();

        val unitCasualties = new HashSet<UnitCasualty>();

        log.debug("Starting to loop through surviving units");
        Arrays.stream(dto.survivingUnits()).forEach(unitDto -> {
            log.debug("Starting calculation for dto [{}]", unitDto);
            log.debug("Checking if unit [{}] is present in army [{}]", unitDto.unitTypeName(), army.getName());
            val foundUnit = units.stream().filter(unit -> unit.getUnitType().getUnitName().equals(unitDto.unitTypeName())).findFirst();

            if(foundUnit.isEmpty()) {
                log.warn("Army [{}] does not contain unit [{}]!", army.getName(), unitDto.unitTypeName());
                throw BattleServiceException.armyDoesNotContainUnit(army.getName(), unitDto.unitTypeName());
            }

            log.debug("Unit [{}] is present in army [{}]", unitDto.unitTypeName(), army.getName());
            val unit = foundUnit.get();
            val oldAmount = unit.getAmountAlive();
            log.debug("Old amount alive of [{}]: [{}]", unitDto.unitTypeName(), oldAmount);
            val newAmount = unitDto.amount();
            log.debug("New amount alive of [{}]: [{}]", unitDto.unitTypeName(), newAmount);

            if(newAmount < 0) {
                log.warn("New amount [{}] is <0", newAmount);
                throw BattleServiceException.newUnitAmountNegative(unitDto.unitTypeName(), newAmount);
            }

            if(newAmount > oldAmount) {
                log.warn("New amount alive [{}] is larger than old amount alive [{}] for unit [{}]!", newAmount, oldAmount, unitDto.unitTypeName());
                throw BattleServiceException.newUnitAmountTooLarge(army.getName(), oldAmount, unitDto.unitTypeName());
            }

            val unitsLost = (long) (oldAmount - newAmount);
            log.debug("Amount of [{}] lost in battle: [{}]", unitDto.unitTypeName(), unitsLost);

            if(unitsLost > 0) {
                log.debug("Amount lost is >0 -> creating UnitCasualty");
                val unitCasualty = new UnitCasualty(unit, unitsLost);

                log.debug("Setting amountAlive of unit [{}] from [{}] to [{}]", unitDto.unitTypeName(), oldAmount, newAmount);
                unit.setAmountAlive(newAmount);

                unitCasualties.add(unitCasualty);
                log.debug("Added UnitCasualty {}", unitCasualty);
            }
        });

        log.debug("Finished updating units");
        log.debug("Created [{}] UnitCasualties", unitCasualties.size());

        return unitCasualties;
    }

    /**
     * Kills all the units in the army and returns a set of UnitCasualties for all the units
     * @param army Army which units should be killed
     * @param battle The battle which the casualties happened in
     * @return A set of UnitCasualty objects of the units that died in the battle.
     */
    private Set<UnitCasualty> killAllUnitsOfArmy(Army army, Battle battle) {
        log.debug("Killing all units of army [{}]", army.getName());

        val unitCasualties = new HashSet<UnitCasualty>();
        val units = army.getUnits();

        units.forEach(unit -> {
            log.debug("Killing unit [{}]", unit);
            val casualty = new UnitCasualty(unit, (long) unit.getAmountAlive());
            log.debug("Created unitCasualty [{}]", casualty);
            unitCasualties.add(casualty);
            log.debug("Setting amountAlive to 0");
            unit.setAmountAlive(0);
        });

        log.debug("Finished killing units");
        log.debug("Created [{}] UnitCasualties", unitCasualties.size());

        return unitCasualties;
    }

    /**
     * Injures every RpChar of the players passed in the DiscordIdDto array.
     * @param dtos Array of discord id dtos of the players that died in the battle
     * @return A Set of RpCharCasualties for every injured RpChar
     * @throws RpCharServiceException noActiveRpChar When player has no RpChar
     */
    private Set<RpCharCasualty> updateKilledPlayers(RpCharCasualtyDto[] dtos) {
        log.debug("Injuring the characters of players: {}", (Object) dtos);

        val allCasualties = new HashSet<RpCharCasualty>();
        Arrays.stream(dtos).forEach(dto -> {
            log.debug("Handling injury of player [{}]", dto.discordId());
            log.debug("Searching player with id [{}]", dto.discordId());
            val player = playerService.getPlayerByDiscordId(dto.discordId());
            val activeChar = player.getActiveCharacter();

            if(activeChar.isEmpty()) {
                log.warn("Player [{}] has no active rpChar that can be injured!", player.getIgn());
                throw RpCharServiceException.noActiveRpChar(player.getIgn());
            }
            val rpChar = activeChar.get();
            log.debug("Found rpChar [{}] for player [{}]", rpChar.getName(), player.getIgn());

            log.debug("Injuring character [{}]", rpChar.getName());
            rpChar.setInjured(true);

            log.debug("Creating RpCharCasualty");
            RpCharCasualty casualty;
            if(StringUtils.isNotBlank((dto.slainByPlayer()))) {
                log.debug("dto.slainByPlayer is set to [{} - fetching slainByPlayer]", dto.slainByPlayer());
                val slainByPlayer = playerService.getPlayerByDiscordId(dto.slainByPlayer());

                String weapon = null;
                if(StringUtils.isNotBlank(dto.slainByWeapon())) {
                    log.debug("dto.slainByWeapon is set to [{}] - additionally adding weapon", dto.slainByWeapon());
                    weapon = dto.slainByWeapon();
                }

                casualty = new RpCharCasualty(rpChar, slainByPlayer, weapon);
            }
            else if (StringUtils.isNotBlank(dto.optionalCause())) {
                log.debug("No slainByPlayer set - using optionalCause");
                casualty = new RpCharCasualty(rpChar, dto.optionalCause());
            }
            else {
                log.debug("No death cause specified");
                casualty = new RpCharCasualty(rpChar);
            }

            log.debug("Adding casualty to list");
            allCasualties.add(casualty);
            log.debug("Finished handling player [{}] ({})", dto.discordId(), player.getIgn());
        });

        log.debug("Finished creating playerCasualties");
        log.debug("Created [{}] casualties", allCasualties.size());

        return allCasualties;
    }
}
