package com.ardaslegends.service.discord.messages.war;

import com.ardaslegends.domain.Army;
import com.ardaslegends.domain.Player;
import com.ardaslegends.domain.RPChar;
import com.ardaslegends.domain.Unit;
import com.ardaslegends.domain.war.battle.Battle;
import com.ardaslegends.domain.war.battle.RpCharCasualty;
import com.ardaslegends.domain.war.battle.UnitCasualty;
import com.ardaslegends.presentation.discord.utils.ALColor;
import com.ardaslegends.presentation.discord.utils.Thumbnails;
import com.ardaslegends.service.discord.DiscordService;
import com.ardaslegends.service.discord.messages.ALMessage;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.mention.AllowedMentions;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.entity.user.User;

import java.util.*;
import java.util.stream.Collectors;

public class BattleMessages {

    public static ALMessage declareBattle(Battle battle, DiscordService discordService) {
        Objects.requireNonNull(battle, "Declare Battle discord message got passed null value for battle");
        Objects.requireNonNull(battle, "Declare Battle discord message got passed null value for discordService");

        AllowedMentions mentions = new AllowedMentionsBuilder()
                .setMentionUsers(true)
                .build();

        val attackingArmy = battle.getInitialAttacker();
        val defendingArmies = battle.getDefendingArmies();
        val isFieldBattle = battle.isFieldBattle();

        MessageBuilder message = new MessageBuilder()
                .setAllowedMentions(mentions)
                .append("The army ")
                .append(attackingArmy.getName())
                .append(" has declared battle on the ")
                .append((isFieldBattle ? "army " :
                        battle.getBattleLocation().getClaimBuild().getOwnedBy().getName() + " " +
                        battle.getBattleLocation().getClaimBuild().getType().getName() + " "))
                .append((isFieldBattle ? battle.getFirstDefender().getName() : battle.getBattleLocation().getClaimBuild().getName()))
                .append("!");

        val embedDescription = message.getStringBuilder().toString();

        if(defendingArmies.stream().anyMatch(army -> army.getBoundTo() != null)) {
            val defendingPlayerMentions = defendingArmies.stream()
                    .map(Army::getBoundTo)
                    .filter(Objects::nonNull)
                    .map(RPChar::getOwner)
                    .map(Player::getDiscordID)
                    .map(discordService::getUserById)
                    .map(User::getMentionTag)
                    .collect(Collectors.joining(", "));

            message.appendNewLine()
                    .append("Defending players: ")
                    .append(defendingPlayerMentions);
        }

        String attackingArmyWithFactionPing = attackingArmy.getName() +
                " (" +
                discordService.getRoleByIdOrElseThrow(attackingArmy.getFaction().getFactionRoleId()).getMentionTag() +
                ")";

        String defendingArmiesWithFactionPings = defendingArmies.stream()
                .map(army -> {
                    val rolePing = discordService.getRoleByIdOrElseThrow(army.getFaction().getFactionRoleId()).getMentionTag();
                    return army.getName() + " (" + rolePing + ")";
                })
                .collect(Collectors.joining("\n"));

        val embed = new EmbedBuilder()
                .setTitle("Battle declared!")
                .setDescription(embedDescription)
                .addInlineField("Battle type", (isFieldBattle ? "Field battle" : "Claimbuild battle"))
                .addInlineField("Region", battle.getBattleLocation().getRegion().getId())
                .addField("", "")
                .addInlineField("Attacking army", attackingArmyWithFactionPing)
                .addInlineField("Defending armies", defendingArmiesWithFactionPings)
                .setColor(ALColor.GREEN)
                .setThumbnail(Thumbnails.DECLARE_BATTLE.getUrl())
                .setTimestampToNow();

        if(!isFieldBattle) {
            embed.addField("","")
                    .addInlineField("Claimbuild", battle.getBattleLocation().getClaimBuild().getName() +
                    " (" + discordService.getRoleByIdOrElseThrow(battle.getBattleLocation().getClaimBuild().getOwnedBy().getFactionRoleId()).getMentionTag()
                    + ")")
                    .addInlineField("Claimbuild Type", battle.getBattleLocation().getClaimBuild().getType().getName());
        }

        val embeds = new ArrayList<EmbedBuilder>();
        embeds.add(embed);

        return new ALMessage(message, embeds);
    }

    public static ALMessage concludeBattle(Battle battle, DiscordService discordService) {
        Objects.requireNonNull(battle, "Conclude Battle discord message got passed null value for battle");
        Objects.requireNonNull(battle, "Conclude Battle discord message got passed null value for discordService");

        AllowedMentions mentions = new AllowedMentionsBuilder()
                .setMentionUsers(true)
                .build();

        MessageBuilder message = new MessageBuilder()
                .setAllowedMentions(mentions)
                .append("The battle ")
                .append(battle.getName())
                .append(" got concluded!");

        val embedDescription = message.getStringBuilder().toString();
        val battleResult = battle.getBattleResult();

        if(!battleResult.getRpCharCasualties().isEmpty()) {
            val injuredPlayerMentions = battleResult.getRpCharCasualties().stream()
                    .map(RpCharCasualty::getRpChar)
                    .map(RPChar::getOwner)
                    .map(Player::getDiscordID)
                    .map(discordService::getUserById)
                    .map(User::getMentionTag)
                    .collect(Collectors.joining(", "));

            message.appendNewLine()
                    .append("Injured players: ")
                    .append(injuredPlayerMentions);
        }

        String attackingArmiesWithFactionPing = battle.getAttackingArmies().stream()
                .map(attackingArmy -> attackingArmy.getName() +
                        " (" +
                        discordService.getRoleByIdOrElseThrow(attackingArmy.getFaction().getFactionRoleId()).getMentionTag() +
                        ")")
                .collect(Collectors.joining("\n"));

        String defendingArmiesWithFactionPings = battle.getDefendingArmies().stream()
                .map(army -> {
                    val rolePing = discordService.getRoleByIdOrElseThrow(army.getFaction().getFactionRoleId()).getMentionTag();
                    return army.getName() + " (" + rolePing + ")";
                })
                .collect(Collectors.joining("\n"));

        val embed = new EmbedBuilder()
                .setTitle("Battle concluded!")
                .setDescription(embedDescription)
                .addInlineField("Battle type", (battle.isFieldBattle() ? "Field battle" : "Claimbuild battle"))
                .addInlineField("Region", battle.getBattleLocation().getRegion().getId())
                .addField("", "")
                .setColor(ALColor.GREEN)
                .setThumbnail(Thumbnails.DECLARE_BATTLE.getUrl())
                .setTimestampToNow();

        if(!battle.isFieldBattle()) {
            embed.addField("","")
                    .addInlineField("Claimbuild", battle.getBattleLocation().getClaimBuild().getName() +
                            " (" + discordService.getRoleByIdOrElseThrow(battle.getBattleLocation().getClaimBuild().getOwnedBy().getFactionRoleId()).getMentionTag()
                            + ")")
                    .addInlineField("Claimbuild Type", battle.getBattleLocation().getClaimBuild().getType().getName());
        }

        embed.addField("", "")
                .addInlineField("Attacking armies", attackingArmiesWithFactionPing)
                .addInlineField("Defending armies", defendingArmiesWithFactionPings);


        val armyCasualtiesEmbed = armyCasualtiesEmbed(battleResult.getUnitCasualties(), discordService);
        val charCasualtiesEmbed = rpCharCasualtiesEmbed(battle.getBattleResult().getRpCharCasualties());

        val embeds = new ArrayList<EmbedBuilder>();
        embeds.add(embed);
        embeds.add(armyCasualtiesEmbed);
        embeds.add(charCasualtiesEmbed);

        return new ALMessage(message, embeds);
    }

    public static EmbedBuilder armyCasualtiesEmbed(Set<UnitCasualty> unitCasualties, DiscordService discordService) {
        Map<Army, List<UnitCasualty>> casualtiesByArmy = unitCasualties.stream()
                .collect(Collectors.groupingBy(UnitCasualty::getArmy));

        val armyString = new StringBuilder();
        val unitString = new StringBuilder();
        val aliveString = new StringBuilder();

        casualtiesByArmy.forEach((army, casualties) -> {
            armyString.append(army.getName())
                    .append(" (")
                    .append(discordService.getRoleByIdOrElseThrow(army.getFaction().getFactionRoleId()).getMentionTag())
                    .append(")")
                    .append("\n".repeat(casualties.size()));

            casualties.forEach(casualty -> {
                unitString.append(casualty.getAmount())
                        .append("x")
                        .append(casualty.getUnit().getUnitType().getUnitName())
                        .append("\n");

                aliveString.append(casualty.getUnit().getAmountAlive())
                        .append("/")
                        .append(casualty.getUnit().getCount())
                        .append("\n");
            });
        });

        if(unitCasualties.isEmpty()) {
            armyString.delete(0, Integer.MAX_VALUE);
            unitString.delete(0, Integer.MAX_VALUE);
            aliveString.delete(0, Integer.MAX_VALUE);

            armyString.append("No units died in this battle!");
        }

        return new EmbedBuilder()
                .setTitle("Army casualties")
                .setDescription("The units that fell in battle")
                .addInlineField("Army", armyString.toString())
                .addInlineField("Casualties", unitString.toString())
                .addInlineField("Now alive", aliveString.toString())
                .setColor(ALColor.GREEN)
                .setTimestampToNow();
    }

    public static EmbedBuilder rpCharCasualtiesEmbed(Set<RpCharCasualty> rpCharCasualties) {

        val charString = new StringBuilder();
        rpCharCasualties.forEach(casualty -> {
            charString.append(casualty.getRpChar().getName())
                    .append(" ");
            if(casualty.getSlainByChar() != null) {
                charString.append("got slain by ");
                if(casualty.getSlainByChar().getActiveCharacter().isPresent()) {
                    charString.append(casualty.getSlainByChar().getActiveCharacter().get().getName());
                }
                else
                    charString.append(casualty.getSlainByChar().getIgn());

                if(StringUtils.isNotBlank(casualty.getSlainByWeapon())) {
                    charString.append(" using ")
                            .append(casualty.getSlainByWeapon());
                }

            }
            else if (StringUtils.isNotBlank(casualty.getOptionalCause())){
                charString.append(casualty.getOptionalCause());
            }
            else
                charString.append("fell in battle");

            charString.append("\n");
        });

        if(rpCharCasualties.isEmpty())
            charString.delete(0, Integer.MAX_VALUE).append("No character has died in this battle!");

        return new EmbedBuilder()
                .setTitle("RpChar casualties")
                .setDescription("The characters that fell in battle")
                .addField("Character", charString.toString())
                .setColor(ALColor.GREEN)
                .setTimestampToNow();
    }
}
