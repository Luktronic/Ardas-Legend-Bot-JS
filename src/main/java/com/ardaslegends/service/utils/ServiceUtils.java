package com.ardaslegends.service.utils;

import com.ardaslegends.domain.Army;
import com.ardaslegends.domain.PathElement;
import com.ardaslegends.domain.Player;
import com.ardaslegends.domain.Region;
import com.ardaslegends.service.exceptions.logic.player.PlayerServiceException;
import com.ardaslegends.service.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ServiceUtils {

    public static <T> void checkBlanks(T obj, List<String> fieldNames) {

        List<Field> fields = null;
        fields = getFieldsFromNames(obj, fieldNames);
        for (var field : fields) {
            field.setAccessible(true);
            if(field.getType().equals(String.class)){
                try {
                    String strField = (String) field.get(obj);
                    checkBlankString(strField, field.getName());
                }
                catch (IllegalAccessException e) {
                    log.warn("Illegal access for object {} and field {}", obj, field);
                }
            }
        }
    }


    public static <T> void checkAllBlanks(T obj) {
        List<String> stringFields = Arrays.stream(obj.getClass().getDeclaredFields())
                .filter(field -> field.getType().equals(String.class))
                .map(Field::getName)
                .collect(Collectors.toList());
        checkBlanks(obj, stringFields);
    }

    public static <T> void checkNulls(T obj, List<String> fieldNames) {
        List<Field> fields = null;
        fields = getFieldsFromNames(obj, fieldNames);
        for (var field : fields) {
            field.setAccessible(true);
            try {
                if(field.get(obj) == null) {
                    log.warn("{} must not be null!", field.getName());
                    throw new NullPointerException("%s must not be null!".formatted(field.getName()));
                }

            }
            catch (IllegalAccessException e) {
                log.warn("Illegal access for object {} and field {}", obj, field);
            }

        }
    }

    public static <T> void checkAllNulls(T obj) {
        checkNulls(obj, Arrays.stream(obj.getClass().getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toList()));
    }

    public static boolean boundLordLeaderPermission(Player player, Army army) {
        log.debug("Checking if bound - lord - leader permission is fulfilled for Army [{}, Faction: {}], Player [{}, Faction:{}]", army.getName(), army.getFaction(), player.getIgn(), player.getFaction());

        if(player.getActiveCharacter().orElseThrow(PlayerServiceException::noRpChar).equals(army.getBoundTo())) {
            //if(player.getActiveCharacter().get().getName().equals(army.getBoundTo().getName())) {
                log.debug("Player [{}] is bound to army, allowed action", player.getIgn());
                return true;
            }


        // TODO: Implement Lordship Permissions

        if(player.equals(army.getFaction().getLeader())) {
            log.debug("Player [{}] is faction leader of army, allowed action", player.getIgn());
            return true;
        }

        log.debug("Player [{}] is not allowed to perform action as per bound - lord - leader permission set!", player.getIgn());
        return false;
    }

    public static void checkBlankString(String value, String fieldName) {
        if(value.isBlank()) {
            log.warn("{} must not be blank!", fieldName);
            throw new IllegalArgumentException("%s must not be blank!".formatted(fieldName));
        }
    }

    private static <T> List<Field> getFieldsFromNames(T obj, List<String> fieldNames) {
        List<Field> fields = new ArrayList<>();
        for (String fieldName : fieldNames) {
            try {
                fields.add(obj.getClass().getDeclaredField(fieldName));
            } catch (NoSuchFieldException e) {
                log.warn("No such field '{}'!", fieldName);
                throw ServiceException.noSuchField(fieldName, e);
            }
        }
        return fields;
    }

    public static Integer getFoodCost(List<PathElement> path) {
        return (int)Math.ceil(ServiceUtils.getTotalPathCost(path) / 24.0);
    }

    public static String formatDuration(Duration duration) {
        Objects.requireNonNull(duration, "Duration must not be null in formatDuration");
        return DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
    }

    // TODO: This needs a rewrite ->
    public static void validateStringSyntax(String string, Character[] syntaxChars, ServiceException exceptionToThrow) {
        log.debug("Validating unitString [{}]", string);

        // Is also true at the start, from then every time a expectedChar is switched
        int currentExpectedCharIndex = 0; //index of the currently expected char
        char expectedChar = syntaxChars[0]; //Set the expectedChar to first in array

        boolean firstCharAfterExpected = true; //says if the current character is the first character after the last expected one
        boolean possibleEnd =
                expectedChar == syntaxChars[syntaxChars.length -1];

        log.trace("Starting validation, unitString length: [{}]", string.length());

        for(int i = 0; i < string.length(); i++) {
            log.trace("Index: [{}]", i);
            log.trace("Expected next syntax char [{}]", expectedChar);
            char currentChar = string.charAt(i);
            log.trace("Current char: [{}]", currentChar);

            if(currentChar == expectedChar) {
                log.trace("Current char {} is expected char {}", currentChar, expectedChar);

                //Check if current char is second last
                possibleEnd = currentExpectedCharIndex == (syntaxChars.length - 2);
                if(syntaxChars.length == 1)
                    possibleEnd = true;
                /*
                Increment the currentExpectedCharIndex so we expect the next character
                If the index has reached the end of the array, start over from 0
                 */
                currentExpectedCharIndex++;
                if(currentExpectedCharIndex == syntaxChars.length)
                    currentExpectedCharIndex = 0;
                log.trace("Incremented the currentExpectedCharIndex to {}", currentExpectedCharIndex);

                expectedChar = syntaxChars[currentExpectedCharIndex];
            }
            else if(Arrays.asList(syntaxChars).contains(currentChar)) {
                log.warn("Char [{}] at [{}] has created an error in string [{}], next expected was [{}]", currentChar, i, string, expectedChar);
                throw exceptionToThrow;
            }


            if((i + 1) == string.length() && !possibleEnd) {
                log.warn("String reached its end without having finished syntax!");
                throw exceptionToThrow;
            }
        }
    }

    public static int getTotalPathCost(List<PathElement> path) {
        return path.stream().map(PathElement::getActualCost).reduce(0, Integer::sum);
    }

    public static String buildPathString(List<PathElement> path) {
        return path.stream().map(PathElement::getRegion).map(Region::getId).collect(Collectors.joining(" -> "));
    }

    public static String buildPathStringWithCurrentRegion(List<PathElement> path, Region current) {
        return buildPathString(path).replace(current.getId(), current.getId() + " (current)");
    }

}
