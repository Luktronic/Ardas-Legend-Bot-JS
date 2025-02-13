package com.ardaslegends.service.exceptions.logic.war;

import com.ardaslegends.domain.Army;
import com.ardaslegends.service.exceptions.logic.LogicException;
import com.ardaslegends.service.utils.ServiceUtils;
import lombok.val;

public class BattleServiceException extends LogicException {
    private static final String FACTIONS_NOT_AT_WAR = "Cannot declare a battle because %s and %s are not at war with each other!";
    private static final String BATTLE_NOT_ABLE_DUE_HOURS = "Region can not be reached within 24 hours";
    private static final String NOT_ENOUGH_HEALTH = "Army does not have enough health";
    private static final String DEFENDING_ARMY_IS_MOVING_AWAY = "The army you are trying to attack is moving away from you and will already reach the next region in %s!";
    private static final String NOT_IN_SAME_REGION = "The army you are trying to attack (%s) is not in the same region as your army (%s)!";
    private static final String ATTACKING_ARMY_HAS_ANOTHER_MOVEMENT = "Attacking army has another ongoing movement!";
    private static final String CANNOT_ATTACK_STARTER_HAMLET = "Starter hamlets cannot be attacked!";
    private static final String NO_PLAYER_BOUND = "The army '%s' must have a player bound to it in order to attack!";
    private static final String ARMY_YOUNGER_THAN_24H = "The army '%s' was created less than 24 hours ago!";
    private static final String FACTION_NOT_PART_IN_BATTLE = "The Faction '%s' is not partaking in battle with id %d!";
    private static final String ARMY_NOT_PART_IN_BATTLE = "The Army '%s' is not partaking in battle with id %d!";
    private static final String BATTLE_ALREADY_CONCLUDED = "This battle is already concluded!";
    private static final String ARMY_DOES_NOT_CONTAIN_UNIT = "Army '%s' does not contain unit '%s'!";
    private static final String UNIT_NEW_AMOUNT_TOO_LARGE = "Army '%s' only had %d '%s' before the battle!";
    private static final String UNIT_NEW_AMOUNT_NEGATIVE = "New '%s' amount cannot be negative (%d)!";

    public static BattleServiceException factionsNotAtWar(String factionName1, String factionName2) { return new BattleServiceException(FACTIONS_NOT_AT_WAR.formatted(factionName1, factionName2)); }

    public static BattleServiceException battleNotAbleDueHours (){ return  new BattleServiceException(BATTLE_NOT_ABLE_DUE_HOURS.formatted());}
    public static BattleServiceException defendingArmyIsMovingAway (Army armyMovingAway){
        val durationUntilNextRegion = armyMovingAway.getActiveMovement().orElseThrow(() -> new IllegalArgumentException("Army %s has no movement! PLEASE CONTACT DEVS".formatted(armyMovingAway.getName()))).getDurationUntilNextRegion();
        return new BattleServiceException(DEFENDING_ARMY_IS_MOVING_AWAY.formatted(ServiceUtils.formatDuration(durationUntilNextRegion)));}
    public static BattleServiceException attackingArmyHasAnotherMovement(){return new BattleServiceException(ATTACKING_ARMY_HAS_ANOTHER_MOVEMENT.formatted());}
    public static BattleServiceException notEnoughHealth(){ return new BattleServiceException(NOT_ENOUGH_HEALTH.formatted());}
    public static BattleServiceException notInSameRegion(Army attacker, Army defender){ return new BattleServiceException(NOT_IN_SAME_REGION.formatted(defender.getName(), attacker.getName()));}
    public static BattleServiceException cannotAttackStarterHamlet() {return new BattleServiceException(CANNOT_ATTACK_STARTER_HAMLET);}
    public static BattleServiceException noPlayerBound(String armyName) {return new BattleServiceException(NO_PLAYER_BOUND.formatted(armyName));}
    public static BattleServiceException armyYoungerThan24h(String armyName) {return new BattleServiceException(ARMY_YOUNGER_THAN_24H.formatted(armyName));}
    public static BattleServiceException factionNotPartOfBattle(String factionName, Long battleId) {return new BattleServiceException(FACTION_NOT_PART_IN_BATTLE.formatted(factionName, battleId));}
    public static BattleServiceException armyNotPartOfBattle(String armyName, Long battleId) {return new BattleServiceException(ARMY_NOT_PART_IN_BATTLE.formatted(armyName, battleId));}
    public static BattleServiceException battleAlreadyConcluded() {return new BattleServiceException(BATTLE_ALREADY_CONCLUDED);}
    public static BattleServiceException armyDoesNotContainUnit(String armyName, String unitName) {return new BattleServiceException(ARMY_DOES_NOT_CONTAIN_UNIT.formatted(armyName, unitName));}
    public static BattleServiceException newUnitAmountTooLarge(String armyName, Integer amountBeforeBattle, String unitName) {return new BattleServiceException(UNIT_NEW_AMOUNT_TOO_LARGE.formatted(armyName, amountBeforeBattle, unitName));}
    public static BattleServiceException newUnitAmountNegative(String unitName, Integer newAmount) {return new BattleServiceException(UNIT_NEW_AMOUNT_NEGATIVE.formatted(unitName, newAmount));}
    protected BattleServiceException(String message, Throwable rootCause) {
        super(message, rootCause);
    }
    protected BattleServiceException(String message) {
        super(message);
    }
}
