package com.ardaslegends.data.presentation.discord.utils;

import com.ardaslegends.data.domain.Army;
import com.ardaslegends.data.domain.ProductionClaimbuild;
import com.ardaslegends.data.domain.SpecialBuilding;
import com.ardaslegends.data.presentation.discord.exception.BotException;
import com.ardaslegends.data.presentation.exceptions.InternalServerException;
import com.ardaslegends.data.service.exceptions.ServiceException;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO: Add Logging
public interface DiscordUtils {

    Logger log = LoggerFactory.getLogger(DiscordUtils.class);

    default String getFullCommandName(SlashCommandInteraction commandInteraction) {
        StringBuilder commandName = new StringBuilder(commandInteraction.getCommandName());

        SlashCommandInteractionOption option = commandInteraction.getOptions().get(0);

        while (option.isSubcommandOrGroup()) {
            commandName.append(" %s".formatted(option.getName()));
            option = option.getOptions().get(0);
        }

        return commandName.toString();
    }

    default List<SlashCommandInteractionOption> getOptions(SlashCommandInteraction interaction) {
        // Returned list of options, initialized with TOP-LEVEL Option list
        List<SlashCommandInteractionOption> optionList = interaction.getOptions();

        SlashCommandInteractionOption option = interaction.getOptions().get(0);

        // Changes the returned list of options if the next option is a subcommand
        // Subcommands are always the first option .get(0) of the level above
        while (option.isSubcommandOrGroup()) {
            log.debug("GetOptions: Option [{}] is subcommand [{}]", option.getName(), option.isSubcommandOrGroup());
            optionList = option.getOptions();
            option = option.getOptions().get(0);
            log.debug("GetOptions: New Option [{}]", option.getName());
        }

        return optionList;
    }

    @NonNull
    default SlashCommandInteractionOption getOption(String name, List<SlashCommandInteractionOption> options) {
        Objects.requireNonNull(name, "GetOption: Name must not be null");
        Objects.requireNonNull(options, "GetOption: Option List must not be null");

        var foundOption = options.stream()
                .filter(interactionOption -> interactionOption.getName().equals(name))
                .findFirst();

        if (foundOption.isEmpty()) {
            log.warn("GetOption: No option with name [{}] found", name);
            throw new RuntimeException("No option with name %s found".formatted(name));
        }

        return foundOption.get();
    }

    default <T> Optional getOptionalValue(String name, List<SlashCommandInteractionOption> options, Class<T> clazz) {

        SlashCommandInteractionOption option = getOption(name, options);

        return switch (clazz.getSimpleName()) {
            case "String" -> option.getStringValue();
            case "User" -> option.getUserValue();
            case "Long" -> option.getLongValue();
            case "Boolean" -> option.getBooleanValue();
            default ->
                    throw new IllegalArgumentException("GetOption: Class Type is not either [Long, String, User, Boolean]!");
        };
    }

    @NonNull
    default <T> T getRequiredValue(String optionName, List<SlashCommandInteractionOption> options, Class<T> clazz) {

        Optional<T> optionalValue = getOptionalValue(optionName, options, clazz);

        if (optionalValue.isEmpty()) {
            log.warn("GetOption: Option [name: {}, type {}] did not return a value!", optionName, clazz.getSimpleName());
            throw new IllegalStateException("GetOption: Option [name: %s, type %s] did not return a value!".formatted(optionName, clazz.getSimpleName()));
        }

        T guaranteedValue = optionalValue.get();
        log.trace("GetOption: Returning option with name [{}] and value [{}]", optionName, guaranteedValue.toString());
        return guaranteedValue;
    }

    default EmbedBuilder createErrorEmbed(String title, String description) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(ALColor.RED)
                .setTimestampToNow();
    }

    @NonNull
    default String getStringOption(String name, List<SlashCommandInteractionOption> options) {
        String optionValue = (String) getRequiredValue(name, options, String.class);
        log.trace("GetStringOption: [{}] Returning value [{}]", name, optionValue);
        return optionValue;
    }

    default Optional<String> getOptionalStringOption(String name, List<SlashCommandInteractionOption> options) {
        Optional<String> option = getOptionalValue(name, options, String.class);
        log.trace("GetOptionalStringOption: [{}] Returning value [{}]", name, option);
        return option;
    }

    default User getUserOption(String name, List<SlashCommandInteractionOption> options) {
        /**Optional<User> foundOption = interaction.getOptionUserValueByName(name);
         if (foundOption.isEmpty()) {
         throw new RuntimeException("No User option with name '%s' found!".formatted(name));
         }

         return foundOption.get();
         **/
        User optionValue = (User) getRequiredValue(name, options, User.class);
        log.trace("GetUserOption: Returning value [{}]", optionValue);
        return optionValue;
    }

    default Optional<User> getOptionalUserOption(String name, List<SlashCommandInteractionOption> options) {
        Optional<User> option = getOptionalValue(name, options, User.class);
        log.trace("GetOptionalUserOption: [{}] Returning value [{}]", name, option);
        return option;
    }

    default Boolean getBooleanOption(String name, List<SlashCommandInteractionOption> options) {
        Boolean optionValue = (Boolean) getRequiredValue(name, options, Boolean.class);
        log.trace("GetBooelanOption: Returning value [{}]", optionValue);
        return optionValue;
    }

    default Optional<Boolean> getOptionalBooleanOption(String name, List<SlashCommandInteractionOption> options) {
        Optional<Boolean> option = getOptionalValue(name, options, Boolean.class);
        log.trace("GetOptionalBooleanOption: [{}] Returning value [{}]", name, option);
        return option;
    }

    default Long getLongOption(String name, List<SlashCommandInteractionOption> options) {
        Long optionValue = (Long) getRequiredValue(name, options, Long.class);
        log.trace("GetOption: Returning value [{}]", optionValue);
        return optionValue;
    }

    default Optional<Long> getOptionalLongOption(String name, List<SlashCommandInteractionOption> options) {
        Optional<Long> option = getOptionalValue(name, options, Long.class);
        log.trace("GetOptionalLongOption: [{}] Returning value [{}]", name, option);
        return option;
    }

    default <T> T getRequiredOption(String optionName, Function<String, Optional<T>> optionFunction) {
        Optional<T> foundOption = optionFunction.apply(optionName);

        if (foundOption.isEmpty()) {
            throw new RuntimeException("No value in option with name '%s' found!".formatted(optionName));
        }

        return foundOption.get();

    }

    default <T> Optional<T> getOptionalOption(String optionName, Function<String, Optional<T>> optionFunction) {
        Optional<T> foundOption = optionFunction.apply(optionName);
        return foundOption;
    }

    default <T, R> R discordServiceExecution(T argument, Function<T, R> function, String errorTitle) {
        if (function == null) {
            throw new InternalServerException("Passed Null SupplierFunction in discordServiceExecution", null);
        }
        try {
            return function.apply(argument);
        } catch (ServiceException e) {
            throw new BotException(errorTitle, e);
        }
    }

    default <T, R, E> R discordServiceExecution(T argument1, E argument2, BiFunction<T, E, R> function, String errorTitle) {
        if (function == null) {
            throw new InternalServerException("Passed Null SupplierFunction in discordServiceExecution", null);
        }
        try {
            return function.apply(argument1, argument2);
        } catch (ServiceException e) {
            throw new BotException(errorTitle, e);
        }
    }

    default String createArmyUnitListString(Army army) {
        StringBuilder unitString = new StringBuilder();
        army.getUnits().stream().forEach(unit -> {
            String unitsAlive = "%d/%d".formatted(unit.getAmountAlive(), unit.getCount());
            String unitName = unit.getUnitType().getUnitName();
            unitString.append(unitsAlive).append(unitName).append("\n");
        });

        return unitString.toString();
    }

    default String createProductionSiteString(List<ProductionClaimbuild> productionSiteList) {
        StringBuilder prodString = new StringBuilder();
        productionSiteList.forEach(productionSite -> {
            String resource = productionSite.getProductionSite().getProducedResource();
            String type = productionSite.getProductionSite().getType().getName();
            int count = productionSite.getCount().intValue();
            prodString.append(count + " " + resource + " " + type + "\n");
        });

        return prodString.toString();
    }

    default String createSpecialBuildingsString(List<SpecialBuilding> specialBuildingList) {
        StringBuilder specialString = new StringBuilder();

        Map<SpecialBuilding, Long> countedSpecialBuildings = specialBuildingList.stream()
                .collect(Collectors.groupingBy(specialBuilding -> specialBuilding, Collectors.counting()));

        countedSpecialBuildings.forEach((specialBuilding, aLong) -> specialString.append(aLong + " " + specialBuilding.getName() + ", "));

        return specialString.toString();
    }

    default Object getFieldValue(String fieldName, Object objectToField) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(objectToField);

        try {
            Field field = objectToField.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(objectToField);
            log.trace("GetFieldValue: Fieldname [{}], value [{}]", fieldName, value.toString());
            return value;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
