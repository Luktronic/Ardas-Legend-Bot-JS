package com.ardaslegends.data.presentation.discord.commands.stockpile.staff;

import com.ardaslegends.data.presentation.discord.commands.ALStaffCommandExecutor;
import com.ardaslegends.data.presentation.discord.config.BotProperties;
import com.ardaslegends.data.presentation.discord.utils.ALColor;
import com.ardaslegends.data.service.FactionService;
import com.ardaslegends.data.service.dto.faction.UpdateStockpileDto;
import lombok.RequiredArgsConstructor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;

import java.util.List;

@RequiredArgsConstructor

public class StockpileAddCommand implements ALStaffCommandExecutor {

    private final FactionService factionService;

    @Override
    public EmbedBuilder execute(SlashCommandInteraction interaction, List<SlashCommandInteractionOption> options, BotProperties properties) {
        log.debug("Incoming /stockpile add request");

        checkStaff(interaction, properties.getStaffRoles());
        log.trace("StockpileAdd: User is staff -> allowed");

        var factionName = getStringOption("faction", options);
        log.trace("StockpileAdd: faction name is [{}]", factionName);

        var amount = getLongOption("amount", options).intValue();
        log.trace("StockpileAdd: amount is [{}]", amount);

        log.trace("StockpileAdd: Building Dto");
        UpdateStockpileDto dto = new UpdateStockpileDto(factionName, amount);

        log.debug("StockpileAdd: Calling discordService");
        var result = discordServiceExecution(dto, factionService::addToStockpile, "Error while updating stockpile");

        log.debug("StockpileAdd: Building Embed");
        return new EmbedBuilder()
                .setTitle("Added to food stockpile")
                .setThumbnail(getFactionBanner(factionName))
                .setTimestampToNow()
                .setDescription("Added %s stacks of food to %s's stockpile".formatted(amount, factionName))
                .addInlineField("Faction", result.getName())
                .addInlineField("Stockpile in Stacks", result.getFoodStockpile().toString())
                .setColor(ALColor.YELLOW);
    }
}
