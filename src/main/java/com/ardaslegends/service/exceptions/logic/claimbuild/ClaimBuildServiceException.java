package com.ardaslegends.service.exceptions.logic.claimbuild;

import com.ardaslegends.service.exceptions.logic.LogicException;

public class ClaimBuildServiceException extends LogicException {

    private static final String NO_CB_WITH_NAME = "Found no claimbuild with name '%s'!";
    private static final String NO_CB_FOR_FACTION = "Faction '%s' has no claimbuilds!";
    private static final String DIFFERENT_FACTION = "The claimbuild '%s' is part of a different faction ('%s') - you cannot interact with it!";
    private static final String DIFFERENT_FACTION_NOT_ALLIED = "The claimbuild '%s' is part of the faction '%s' - you are not allied with '%s' and therefore cannot interact with it!";
    private static final String REGION_IS_NOT_CLAIMABLE_FOR_FACTION = "Region '%s' is not claimable for the faction '%s' because the region is not unclaimed and the faction does not have a Claimbuild already in that region.";

    //Create claimbuild
    private static final String CB_ALREADY_EXISTS = "A claimbuild with the name '%s' already exists in Region '%s' (owned by %s)";
    private static final String COULD_NOT_UPDATE_CB_BECAUSE_IT_DOES_NOT_EXIST = "Could not update claimbuild because no claimbuild with name '%s' was found!";
    private static final String NO_CLAIMBUILD_TYPE_FOUND = "No Claimbuild Type found for inputted value '%s'!";
    private static final String FACTION_ALREADY_HAS_CAPITAL = "The faction '%s' already has a Capital claimbuild!";
    private static final String INVALID_PRODUCTION_SITE_STRING = "The production site string '%s' is not grammatically correct \n " +
            "A correct string would be: Fishing Lodge:Salmon:2-Mine:Iron:5 \n" +
            "Actual Grammar=[Type]:[Resource]:[Amount]-[Type2]:[Resource2]:[Amount2]";
    private static final String NO_PRODUCTION_SITE_TYPE_FOUND = "No Production Site Type found for inputted value '%s'!";
    private static final String NO_SPECIAL_BUILDING_FOUND = "No Special Building found for inputted value '%s'!";
    private static final String NO_PRODUCTION_SITE_FOUND = "No Production Site with type '%s' and resource '%s' found!\n" +
            "Pay attention to upper and lower case!";
    private static final String INVALID_BUILT_BY_STRING = "The 'built by' string '%s' is not grammatically correct \n " +
            "A correct string would be: Player1-Player2-Player3 \n" +
            "Actual Grammar=[Player]-[Player2]-[Player3]";

    public static ClaimBuildServiceException noCbWithName(String cbName) { return new ClaimBuildServiceException(NO_CB_WITH_NAME.formatted(cbName)); }
    public static ClaimBuildServiceException factionHasNoCbs(String factionName) { return new ClaimBuildServiceException(NO_CB_FOR_FACTION.formatted(factionName)); }
    public static ClaimBuildServiceException differentFaction(String cbName, String factionName) { return new ClaimBuildServiceException(DIFFERENT_FACTION.formatted(cbName, factionName)); }
    public static ClaimBuildServiceException differentFactionNotAllied(String cbName, String factionName) { return new ClaimBuildServiceException(DIFFERENT_FACTION_NOT_ALLIED.formatted(cbName, factionName, factionName)); }
    public static ClaimBuildServiceException regionIsNotClaimableForFaction(String regionId, String factionName) {return new ClaimBuildServiceException(REGION_IS_NOT_CLAIMABLE_FOR_FACTION.formatted(regionId,factionName));}

    //Create claimbuild
    public static ClaimBuildServiceException cbAlreadyExists(String cbName, String regionId, String factionName) { return new ClaimBuildServiceException(CB_ALREADY_EXISTS.formatted(cbName, regionId, factionName)); }
    public static ClaimBuildServiceException couldNotUpdateClaimbuildBecauseNotFound(String cbName) { return new ClaimBuildServiceException(COULD_NOT_UPDATE_CB_BECAUSE_IT_DOES_NOT_EXIST.formatted(cbName)); }
    public static ClaimBuildServiceException noCbTypeFound(String inputtedType) { return new ClaimBuildServiceException(NO_CLAIMBUILD_TYPE_FOUND.formatted(inputtedType)); }
    public static ClaimBuildServiceException factionAlreadyHasCapital(String factionName) { return new ClaimBuildServiceException(FACTION_ALREADY_HAS_CAPITAL.formatted(factionName)); }
    public static ClaimBuildServiceException invalidProductionSiteString(String string) { return new ClaimBuildServiceException(INVALID_PRODUCTION_SITE_STRING.formatted(string)); }
    public static ClaimBuildServiceException noProductionSiteTypeFound(String inputtedValue) { return new ClaimBuildServiceException(NO_PRODUCTION_SITE_TYPE_FOUND.formatted(inputtedValue)); }
    public static ClaimBuildServiceException noSpecialBuildingFound(String inputtedValue) { return new ClaimBuildServiceException(NO_SPECIAL_BUILDING_FOUND.formatted(inputtedValue)); }
    public static ClaimBuildServiceException invalidBuiltByString(String string) { return new ClaimBuildServiceException(INVALID_BUILT_BY_STRING.formatted(string)); }
    public static ClaimBuildServiceException noProductionSiteFound(String prodType, String resource) { return new ClaimBuildServiceException(NO_PRODUCTION_SITE_FOUND.formatted(prodType, resource)); }

    protected ClaimBuildServiceException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    protected ClaimBuildServiceException(String message) {
        super(message);
    }
}
