package com.ardaslegends.presentation.discord.commands.disband;

import com.ardaslegends.domain.Army;
import com.ardaslegends.domain.ArmyType;
import com.ardaslegends.presentation.discord.commands.ALCommandExecutor;
import com.ardaslegends.presentation.discord.commands.ALMessageResponse;
import com.ardaslegends.presentation.discord.config.BotProperties;
import com.ardaslegends.presentation.discord.utils.ALColor;
import com.ardaslegends.presentation.discord.utils.Thumbnails;
import com.ardaslegends.service.ArmyService;
import com.ardaslegends.service.dto.army.DeleteArmyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;

import java.util.List;

@RequiredArgsConstructor

@Slf4j
public class DisbandArmyOrcompanyCommand implements ALCommandExecutor {

    private final ArmyService armyService;

    @Override
    public ALMessageResponse execute(SlashCommandInteraction interaction, List<SlashCommandInteractionOption> options, BotProperties properties) {
        log.debug("Executing /disband army-or-company request");

        User user = interaction.getUser();
        log.debug("User: discord name [{}] - id [{}]", user.getName(), user.getIdAsString());

        log.debug("Getting options");
        String armyName = getStringOption("army-or-company-name", options);
        log.debug("armyName: [{}]", armyName);

        log.trace("Building dto");
        DeleteArmyDto dto = new DeleteArmyDto(user.getIdAsString(), armyName);
        log.debug("Built dto with data [{}]", dto);

        log.trace("Calling armyService");
        Army army = discordServiceExecution(dto, false, armyService::disbandFromDto, "Error while disbanding Army/Company");
        String armyType = army.getArmyType().getName();

        String thumbnail = "";
        if (army.getArmyType() == ArmyType.ARMY) {
            thumbnail = getFactionBanner(army.getFaction().getName());
        } else {
            thumbnail = Thumbnails.BIND_TRADER.getUrl();
        }

        log.debug("Building response Embed");
        return new ALMessageResponse(null, new EmbedBuilder()
                .setTitle("Disbanded %s".formatted(armyType))
                .setDescription("The %s %s has been disbanded".formatted(armyType, army.getName()))
                .setColor(ALColor.GREEN)
                .addInlineField("%s".formatted(armyType), army.getName())
                .addInlineField("%s Faction".formatted(armyType), army.getFaction().getName())
                .addInlineField("Was in Region", army.getCurrentRegion().getId())
                .addInlineField("Created from Claimbuild", army.getOriginalClaimbuild().getName())
                .setThumbnail(thumbnail)
                .setTimestampToNow());
    }
}
