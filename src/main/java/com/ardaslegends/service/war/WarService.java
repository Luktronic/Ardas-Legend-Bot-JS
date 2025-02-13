package com.ardaslegends.service.war;

import com.ardaslegends.domain.Faction;
import com.ardaslegends.domain.war.War;
import com.ardaslegends.repository.exceptions.NotFoundException;
import com.ardaslegends.repository.faction.FactionRepository;
import com.ardaslegends.repository.war.WarRepository;
import com.ardaslegends.service.AbstractService;
import com.ardaslegends.service.PlayerService;
import com.ardaslegends.service.discord.DiscordService;
import com.ardaslegends.service.discord.messages.war.WarMessages;
import com.ardaslegends.service.dto.war.CreateWarDto;
import com.ardaslegends.service.dto.war.EndWarDto;
import com.ardaslegends.service.exceptions.logic.faction.FactionServiceException;
import com.ardaslegends.service.exceptions.logic.war.WarServiceException;
import com.ardaslegends.service.exceptions.permission.StaffPermissionException;
import com.ardaslegends.service.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class WarService extends AbstractService<War, WarRepository> {
    private final WarRepository warRepository;
    private final FactionRepository factionRepository;
    private final PlayerService playerService;
    private final DiscordService discordService;

    public Page<War> getWars(Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable getWarsBody must not be null");
        var page = secureFind(pageable, warRepository::findAll);
        return page;
    }

    @Transactional(readOnly = false)
    public War createWar(CreateWarDto createWarDto) {
        log.debug("Creating war with data [defender: {}]", createWarDto);

        Objects.requireNonNull(createWarDto, "CreateWarDto must not be null");
        Objects.requireNonNull(createWarDto.executorDiscordId(), "ExecutorDiscordId must not be null");
        Objects.requireNonNull(createWarDto.nameOfWar(), "Name of War must not be null");
        Objects.requireNonNull(createWarDto.defendingFactionName(), "Defending Faction Name must not be null");

        val activeWarWithName = warRepository.queryActiveWarByName(createWarDto.nameOfWar());
        if(activeWarWithName.isPresent()) {
            log.warn("Cannot create war because active war with name [{}] already exists!", createWarDto.nameOfWar());
            throw WarServiceException.activeWarAlreadyExists(createWarDto.nameOfWar());
        }

        log.trace("Fetching player with discordId [{}]", createWarDto.executorDiscordId());
        var executorPlayer = playerService.getPlayerByDiscordId(createWarDto.executorDiscordId());

        Faction attackingFaction = executorPlayer.getFaction();
        log.trace("Attacking faction is [{}]", attackingFaction.getName());

        // TODO: This should include lords who also can declare wars
        if(!attackingFaction.getLeader().equals(executorPlayer)) {
            log.warn("Player [{}] does not have the permission to declare war!", executorPlayer.getIgn());
            throw WarServiceException.noWarDeclarationPermissions();
        }

        log.trace("Fetching defending Faction with name [{}]", createWarDto.defendingFactionName());
        var fetchedDefendingFaction = secureFind(createWarDto.defendingFactionName(), factionRepository::findFactionByName);

        if(fetchedDefendingFaction.isEmpty()) {
            log.warn("No defending faction found with name [{}]", createWarDto.defendingFactionName());
            // TODO: This is stupid as fuck, find other solution
            List<Faction> allFactions = secureFind(factionRepository::findAll);
            String allFactionString = allFactions.stream().map(Faction::getName).collect(Collectors.joining(", "));
            throw FactionServiceException.noFactionWithNameFoundAndAll(createWarDto.defendingFactionName(),allFactionString);
        }

        var defendingFaction = fetchedDefendingFaction.get();

        if(attackingFaction.equals(defendingFaction)) {
            log.warn("Player [{}] tried to declare war on his faction", executorPlayer.getIgn());
            throw WarServiceException.cannotDeclareWarOnYourFaction();
        }

        warRepository.queryActiveInitialWarBetween(attackingFaction, defendingFaction).ifPresent(war -> {
            log.warn("The factions '{}' and '{}' are already at war!", attackingFaction.getName(), defendingFaction.getName());
            throw WarServiceException.alreadyAtWar(attackingFaction.getName(), defendingFaction.getName());
        });

        War war = new War(createWarDto.nameOfWar(), attackingFaction, defendingFaction);

        log.debug("Saving War Entity");
        war = secureSave(war, warRepository);

        discordService.sendMessageToRpChannel(WarMessages.declareWar(war, discordService));

        log.info("Successfully executed and saved new war {}", war.getName());
        return war;
    }

    public Set<War> getActiveWarsOfFaction(String factionName) {
        // TODO, not yet implemented
        return null;
    }

    public Set<War> getActiveWarsOfFaction(Faction faction) {
        Set<War> wars = secureFind(faction, warRepository::findAllActiveWarsWithFaction);
        return wars;
    }

    @Transactional(readOnly = false)
    public War forceEndWar(EndWarDto dto) {
        log.debug("Player with discord id [{}] is trying to force end war [{}]", dto.executorDiscordId(), dto.warName());

        log.debug("Checking nulls and blanks");
        ServiceUtils.checkAllNulls(dto);
        ServiceUtils.checkAllBlanks(dto);

        log.trace("Fetching player with discord id [{}]", dto.executorDiscordId());
        val player = playerService.getPlayerByDiscordId(dto.executorDiscordId());

        log.debug("DiscordId [{}] belongs to player [{}]", dto.executorDiscordId(), player.getIgn());

        log.debug("Player [{}] is staff: {}", player.getIgn(), player.getIsStaff());
        if(!player.getIsStaff()) {
            log.warn("Player [{}] is not a staff member and does not have the permission to force end wars!", player.getIgn());
            throw StaffPermissionException.noStaffPermission();
        }

        val war = getActiveWarByName(dto.warName());
        log.debug("Found war with name [{}]", war.getName());

        log.debug("Ending war");
        war.end();

        discordService.sendMessageToRpChannel(WarMessages.forceEndWar(war, player, discordService));

        log.info("War [{}] between attacker [{}] and defender [{}] has succesfully been ended by staff member [{}]", war.getName(), war.getInitialAttacker().getName(), war.getInitialDefender().getName(), player.getIgn());
        return war;
    }

    public Set<War> getWarsByName(String name) {
        log.debug("Getting wars with name [{}]", name);

        Objects.requireNonNull(name, "War name must not be null!");
        ServiceUtils.checkBlankString(name, "name");

        log.debug("Fetching wars with name [{}]", name);
        val foundWars = secureFind(name, warRepository::findByName);

        if(foundWars.isEmpty()) {
            log.warn("Found no wars with name [{}]", name);
            throw NotFoundException.noWarWithNameFound(name);
        }

        foundWars.forEach(war -> {
            log.debug("Found war [{}] between attacker [{}] and defender [{}]", war.getName(), war.getInitialAttacker().getWarParticipant().getName(), war.getInitialDefender().getWarParticipant().getName());
        });
        return foundWars;
    }

    public War getActiveWarByName(String name) {
        log.debug("Getting active war with name [{}]", name);
        val foundWar = warRepository.queryActiveWarByName(name);

        if(foundWar.isEmpty()) {
            log.warn("Found no war with name [{}]!", name);
            throw NotFoundException.noActiveWarWithNameFound(name);
        }
        val war = foundWar.get();
        log.info("Found active war with name [{}] between attacker [{}] and defender [{}]", war.getName(), war.getInitialAttacker().getWarParticipant().getName(), war.getInitialDefender().getWarParticipant().getName());
        return war;
    }

}
