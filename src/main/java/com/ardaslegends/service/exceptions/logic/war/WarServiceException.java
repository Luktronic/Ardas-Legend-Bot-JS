package com.ardaslegends.service.exceptions.logic.war;

import com.ardaslegends.service.exceptions.logic.LogicException;
import com.ardaslegends.service.war.WarService;

public class WarServiceException extends LogicException {

    private static final String NO_WAR_DECLARATION_PERMISSIONS = "You are not a faction leader or lord with permission to declare wars!";
    private static final String FACTION_ALREADY_JOINED_THE_WAR_AS_ATTACKER = "%s already joined the war as an attacking faction";
    private static final String FACTION_ALREADY_JOINED_THE_WAR_AS_DEFENDER = "%s already joined the war as a defending faction";
    private static final String THIS_BATTLE_IS_ALREADY_IN_THE_BATTLE_LIST = "Battle [%s] is already listed in the battles of this war";
    private static final String CANNOT_DECLARE_WAR_ON_YOUR_FACTION = "You cannot declare war on your own faction!";
    private static final String ALREADY_AT_WAR = "Your faction '%s' is already at war with '%s'!";
    private static final String WAR_NOT_ACTIVE = "The war '%s' is already over!";
    private static final String ACTIVE_WAR_ALREADY_EXISTS = "There is already an active war called '%s'!";

    public static WarServiceException noWarDeclarationPermissions() { return new WarServiceException(NO_WAR_DECLARATION_PERMISSIONS); }
    public static WarServiceException factionAlreadyJoinedTheWarAsAttacker(String factionName) { return new WarServiceException(FACTION_ALREADY_JOINED_THE_WAR_AS_ATTACKER.formatted(factionName));}
    public static WarServiceException factionAlreadyJoinedTheWarAsDefender(String factionName) { return new WarServiceException(FACTION_ALREADY_JOINED_THE_WAR_AS_DEFENDER.formatted(factionName));}
    public static WarServiceException battleAlreadyListed(String battleName) { return new WarServiceException(THIS_BATTLE_IS_ALREADY_IN_THE_BATTLE_LIST.formatted(battleName));}
    public static WarServiceException cannotDeclareWarOnYourFaction() { return new WarServiceException(CANNOT_DECLARE_WAR_ON_YOUR_FACTION); }
    public static WarServiceException alreadyAtWar(String executorFaction, String otherFaction) { return new WarServiceException(ALREADY_AT_WAR.formatted(executorFaction, otherFaction)); }
    public static WarServiceException warNotActive(String name) {return new WarServiceException(WAR_NOT_ACTIVE.formatted(name));}
    public static WarServiceException activeWarAlreadyExists(String name) {return new WarServiceException(ACTIVE_WAR_ALREADY_EXISTS.formatted(name));}

    public WarServiceException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    public WarServiceException(String message) {
        super(message);
    }
}
