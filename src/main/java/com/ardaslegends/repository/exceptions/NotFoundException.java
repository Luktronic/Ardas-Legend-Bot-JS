package com.ardaslegends.repository.exceptions;

public class NotFoundException extends RepositoryException {


    private static final String GENERIC_COULD_NOT_FIND = "Could not find %s with %s %s!";
    private static final String NO_WAR_WITH_NAME = "No war with name '%s' found!";
    private static final String NO_ACTIVE_WAR_WITH_NAME = "No active war with name '%s' found!";

    protected NotFoundException(String message) {
        super(message);
    }

    protected NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Results in an error with format "Could not find %s with %s %s!"
     */
    public static NotFoundException genericNotFound(String couldNotFind, String with, String value) {
        return new NotFoundException(GENERIC_COULD_NOT_FIND.formatted(couldNotFind, with, value));
    }
    public static NotFoundException noWarWithNameFound(String name) {return new NotFoundException(NO_WAR_WITH_NAME.formatted(name));}
    public static NotFoundException noActiveWarWithNameFound(String name) {return new NotFoundException(NO_ACTIVE_WAR_WITH_NAME.formatted(name));}


}
