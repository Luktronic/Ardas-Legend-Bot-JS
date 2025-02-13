package com.ardaslegends.presentation.discord.commands.delete.staff;

import com.ardaslegends.domain.ArmyType;
import com.ardaslegends.presentation.discord.commands.ALMessageResponse;
import com.ardaslegends.presentation.discord.commands.ALStaffCommandExecutor;
import com.ardaslegends.presentation.discord.config.BotProperties;
import com.ardaslegends.presentation.discord.utils.ALColor;
import com.ardaslegends.presentation.discord.utils.Thumbnails;
import com.ardaslegends.service.ArmyService;
import com.ardaslegends.service.dto.army.DeleteArmyDto;
import lombok.RequiredArgsConstructor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor

@Component
public class DeleteArmyOrCompanyCommand implements ALStaffCommandExecutor {

    private final ArmyService armyService;

    @Override
    public ALMessageResponse execute(SlashCommandInteraction interaction, List<SlashCommandInteractionOption> options, BotProperties properties) {
        log.debug("Handling /delete army-or-company request");

        checkStaff(interaction, properties.getStaffRoleIds());
        log.trace("DeleteArmy: User is staff -> authorized");

        String armyName = getStringOption("army-or-company-name", options);
        log.trace("DeleteArmy: armyName value is [{}]", armyName);

        log.trace("DeleteArmy: Building Dto");
        DeleteArmyDto dto = new DeleteArmyDto(interaction.getUser().getIdAsString(), armyName);

        log.debug("DeleteArmy: Calling Service Execution");
        var army = discordServiceExecution(dto, true, armyService::disbandFromDto, "Error while deleting army");

        String armyType = army.getArmyType().getName();

        String thumbnail = "";
        if (army.getArmyType() == ArmyType.ARMY) {
            thumbnail = getFactionBanner(army.getFaction().getName());
        } else {
            thumbnail = Thumbnails.BIND_TRADER.getUrl();
        }

        return new ALMessageResponse(null, new EmbedBuilder()
                .setTitle("Staff-Deleted %s".formatted(armyType))
                .setDescription("Deleted %s '%s' from the game!".formatted(armyType, army.getName()))
                .setColor(ALColor.YELLOW)
                .addInlineField("%s".formatted(armyType), army.getName())
                .addInlineField("%s Faction".formatted(armyType), army.getFaction().getName())
                .addInlineField("Was in Region", army.getCurrentRegion().getId())
                .addInlineField("Created from Claimbuild", army.getOriginalClaimbuild().getName())
                .setThumbnail(thumbnail)
                .setTimestampToNow());
    }
}
