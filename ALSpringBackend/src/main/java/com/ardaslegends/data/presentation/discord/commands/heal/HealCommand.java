package com.ardaslegends.data.presentation.discord.commands.heal;

import com.ardaslegends.data.presentation.discord.commands.ALCommand;
import com.ardaslegends.data.presentation.discord.commands.ALCommandExecutor;
import com.ardaslegends.data.presentation.discord.commands.injure.InjureRpcharCommand;
import com.ardaslegends.data.service.ArmyService;
import com.ardaslegends.data.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@RequiredArgsConstructor

@Slf4j
@Component
public class HealCommand implements ALCommand {

    private final PlayerService playerService;
    private final ArmyService armyService;

    @Override
    public SlashCommandBuilder init(Map<String, ALCommandExecutor> commands) {
        log.debug("Initializing /heal command");

        var command = SlashCommand.with("heal", "JAVACORD Starts healing a character, army, etc.", Arrays.asList(
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("rpchar")
                        .setDescription("Starts healing your injured character")
                        .build()
        ));
        commands.put("heal rpchar", new HealRpcharCommand(playerService));
        log.info("Finished initializing /heal command");
        return command;
    }
}
