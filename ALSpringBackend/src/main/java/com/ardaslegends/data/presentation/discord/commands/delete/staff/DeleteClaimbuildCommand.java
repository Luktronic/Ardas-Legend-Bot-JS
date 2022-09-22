package com.ardaslegends.data.presentation.discord.commands.delete.staff;

import com.ardaslegends.data.domain.Army;
import com.ardaslegends.data.domain.ClaimBuild;
import com.ardaslegends.data.presentation.discord.commands.ALCommandExecutor;
import com.ardaslegends.data.presentation.discord.commands.ALStaffCommand;
import com.ardaslegends.data.presentation.discord.config.BotProperties;
import com.ardaslegends.data.presentation.discord.utils.ALColor;
import com.ardaslegends.data.presentation.discord.utils.DiscordUtils;
import com.ardaslegends.data.service.ClaimBuildService;
import com.ardaslegends.data.service.dto.claimbuilds.DeleteClaimbuildDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor

@Component
public class DeleteClaimbuildCommand implements ALCommandExecutor, ALStaffCommand,DiscordUtils {

    private final ClaimBuildService claimBuildService;

    @Override
    public EmbedBuilder execute(SlashCommandInteraction interaction, List<SlashCommandInteractionOption> options, BotProperties properties) {
        log.debug("Incoming /delete claimbuild request, getting option-data");

        checkStaff(interaction, properties.getStaffRoles());

        log.debug("Fetching option-data");
        String claimbuildName = getStringOption("claimbuild-name", options);

        DeleteClaimbuildDto dto = new DeleteClaimbuildDto(claimbuildName, null ,null);
        var claimbuild = discordServiceExecution(dto, claimBuildService::deleteClaimbuild, "Error during deletion of claimbuild");
        log.debug("DeleteClaimbuild: Result [{}]", claimbuild);

        String deletedArmies = claimbuild.getCreatedArmies().stream().map(Army::getName).collect(Collectors.joining(", "));
        String unstationedArmies = claimbuild.getStationedArmies().stream().map(Army::getName).collect(Collectors.joining(", "));

        return new EmbedBuilder()
                .setTitle("Staff-Deleted Claimbuild")
                .setColor(ALColor.YELLOW)
                .setDescription("Claimbuild %s of faction %s has been deleted".formatted(claimbuildName, claimbuild.getOwnedBy().getName()))
                .addField("Unstationed Armies/Companies", unstationedArmies.isEmpty() ? "None":unstationedArmies)
                .addField("Deleted Armies/Companies", deletedArmies.isEmpty() ? "None":deletedArmies)
                .setTimestampToNow();
    }
}
