package com.ardaslegends.service.applications;

import com.ardaslegends.domain.applications.ApplicationState;
import com.ardaslegends.domain.applications.ClaimbuildApplication;
import com.ardaslegends.repository.claimbuild.ClaimbuildRepository;
import com.ardaslegends.repository.player.PlayerRepository;
import com.ardaslegends.repository.applications.claimbuildapp.ClaimbuildApplicationRepository;
import com.ardaslegends.service.AbstractService;
import com.ardaslegends.service.dto.applications.CreateClaimbuildApplicationDto;
import com.ardaslegends.service.dto.player.DiscordIdDto;
import com.ardaslegends.service.exceptions.applications.ClaimbuildApplicationException;
import com.ardaslegends.service.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@RequiredArgsConstructor
@Slf4j

@Service
@Transactional(readOnly = true)
public class ClaimbuildApplicationService extends AbstractService<ClaimbuildApplication, ClaimbuildApplicationRepository> {

    private final ClaimbuildApplicationRepository cbAppRepository;
    private final ClaimbuildRepository claimBuildRepository;
    private final PlayerRepository playerRepository;

    @Transactional(readOnly = false)
    public ClaimbuildApplication createClaimbuildApplication(CreateClaimbuildApplicationDto dto) {
        log.debug("Creating ClaimbuildApplication with data [{}]", dto);
        Objects.requireNonNull(dto);
        ServiceUtils.checkAllNulls(dto);

        val applicantPlayer = playerRepository.queryByDiscordId(dto.applicant().discordId());

        // Check if CB with Name already exists, throw if so
        if(claimBuildRepository.existsByNameIgnoreCase(dto.claimbuildName())) {
            log.warn("Claimbuild with name [{}] already exists", dto.claimbuildName());
            throw ClaimbuildApplicationException.claibuildWithNameAlreadyExists(dto.claimbuildName());
        }

        // Check if CBApp with Name already exists that is active, throw if so
        if(cbAppRepository.existsByNameIgnoreCaseAndState(dto.claimbuildName(), ApplicationState.OPEN)) {
            log.warn("Claimbuild Application with name [{}] already exists", dto.claimbuildName());
            throw ClaimbuildApplicationException.claibuildApplicationWithNameAlreadyExists(dto.claimbuildName());
        }

        val foundPlayers = playerRepository.queryByDiscordId(Arrays.stream(dto.builtBy()).map(DiscordIdDto::discordId).toArray(String[]::new));

        // Iterating over initial discordId which should be present in foundPlayers and mapping which Ids have not been found.
        List<String> notFoundPlayersIds = Arrays.stream(dto.builtBy())
                .map(DiscordIdDto::discordId)
                .filter(discordId -> foundPlayers.stream().noneMatch(player -> player.getDiscordID().equals(discordId)))
                .toList();

        if(!notFoundPlayersIds.isEmpty()) {
            String playersNotFound = String.join(", ", notFoundPlayersIds);
            log.warn("ClaimbuildApplicationService: Failed to find following builders [{}]", playersNotFound);
            throw ClaimbuildApplicationException.buildersNotFound(playersNotFound);
        }


        // Figure out what to do with this....


        return null;
    }
}
