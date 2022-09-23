package com.ardaslegends.data.presentation.discord.commands;

import com.ardaslegends.data.presentation.discord.commands.bind.Bind;
import com.ardaslegends.data.presentation.discord.commands.create.CreateCommand;
import com.ardaslegends.data.presentation.discord.commands.delete.DeleteCommand;
import com.ardaslegends.data.presentation.discord.commands.move.MoveCommand;
import com.ardaslegends.data.presentation.discord.commands.register.RegisterCommand;
import com.ardaslegends.data.presentation.discord.commands.update.UpdateCommand;
import com.ardaslegends.data.presentation.discord.config.BotProperties;
import com.ardaslegends.data.presentation.discord.utils.DiscordUtils;
import com.ardaslegends.data.presentation.discord.exception.BotException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Component
public class Commands implements DiscordUtils {

    private final DiscordApi api;
    private final Bind bind;
    private final RegisterCommand register;
    private final CreateCommand create;
    private final DeleteCommand delete;
    private final Map<String, ALCommandExecutor> executions;

    private final BotProperties properties;
    public Commands(DiscordApi api, Bind bind, RegisterCommand register, CreateCommand create, DeleteCommand delete, BotProperties properties,
        UpdateCommand update, MoveCommand move
    ) {
        this.api = api;
        this.bind = bind;
        this.register = register;
        this.create = create;
        this.delete = delete;
        this.properties = properties;

        executions = new HashMap<>();
        bind.init(executions);
        register.init(executions);
        create.init(executions);
        delete.init(executions);
        update.init(executions);
        move.init(executions);

        log.debug("Fetching roleplay-commands channel with ID in Property file");
        Channel rpCommandsChannel = api.getChannelById(properties.getRpCommandsChannel()).orElseThrow();

        api.addSlashCommandCreateListener(event -> {

            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            try {

                var responseUpdater = interaction.respondLater().join();

                EmbedBuilder embed;
                try {
                    String fullname = getFullCommandName(interaction);
                    log.trace("Full CommandName: [{}]", fullname);
                    List<SlashCommandInteractionOption> options = getOptions(interaction);

                    log.trace("List of available options: {}", options.stream()
                            .map(interactionOption -> interactionOption.getName())
                            .collect(Collectors.joining(", ")));

                    log.info("Incoming '/{}' command", fullname);
                    log.trace("Calling command execution function");
                    embed = executions.get(fullname).execute(interaction, options, properties);

                    log.info("Finished handling '/{}' command", fullname);
                } catch (BotException exception) {
                    log.warn("Encountered ServiceException while executing, msg: {}", exception.getMessage());
                    embed = createErrorEmbed(exception.getTitle(), exception.getMessage());
                } catch (Exception exception) {
                    log.error("ENCOUNTERED UNEXPECTED ERROR OF TYPE {} - MSG: {}", exception.getClass(), exception.getMessage());
                    String message = exception.getMessage() + "\nPlease contact the devs!";
                    embed = createErrorEmbed("An unexpected error occured", message);
                }

                log.debug("Updating response to new embed");
                // The join() is important so that the exceptions go into the catch blocks
                responseUpdater.addEmbed(embed).update().join();
            } catch (Exception e) {
                // TODO: Create Error Report of Stacktrace and stuff, until then we're throwing this again
                // TODO: This does not
                e.printStackTrace();
            }
        });
    }

}
