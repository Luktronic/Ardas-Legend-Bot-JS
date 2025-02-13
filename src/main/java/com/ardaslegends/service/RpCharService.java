package com.ardaslegends.service;

import com.ardaslegends.domain.Army;
import com.ardaslegends.domain.RPChar;
import com.ardaslegends.repository.rpchar.RpcharRepository;
import com.ardaslegends.service.exceptions.logic.rpchar.RpCharServiceException;
import com.ardaslegends.service.utils.ServiceUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor

@Service
public class RpCharService extends AbstractService<RPChar, RpcharRepository>{

    private final RpcharRepository rpcharRepository;

    public Slice<RPChar> getAll(Pageable pageable) {
        log.info("Getting slice of rpchars with data [{}]", pageable);
        return rpcharRepository.queryAll(pageable);
    }

    public List<RPChar> getRpCharsByNames(String[] names) {
        log.debug("Getting RpChars with names [{}]", (Object) names);

        Objects.requireNonNull(names, "Names must not be null");
        Arrays.stream(names).forEach(str -> ServiceUtils.checkBlankString(str, "Name"));

        log.debug("Fetching RpChars with names [{}]", (Object) names);
        List<RPChar> fetchedChars = secureFind(names, rpcharRepository::findRpCharsByNames);

        if(fetchedChars.isEmpty()) {
            log.warn("No RpChars found with names [{}]", (Object) names);
            throw RpCharServiceException.noRpCharsFound(names);
        }

        log.debug("Successfully returning RPChars found with names [{}]", (Object) names);
        return fetchedChars;
    }

    public RPChar getRpCharByName(@NonNull String name) {
        log.debug("Getting RpChar with name [{}]", name);

        log.debug("Fetching Rpchar with name [{}]", name);
        val character = rpcharRepository.findRpcharByName(name);

        if(character.isEmpty()) {
            log.warn("No RpChar found with name [{}]", name);
            throw RpCharServiceException.noRpCharFound(name);
        }

        log.debug("Successfully returning RpChar with name [{}]", name);
        return character.get();
    }

    public List<RPChar> saveRpChars(List<RPChar> chars) {
        log.debug("Saving chars [{}]", chars);
        return secureSaveAll(chars, rpcharRepository);
    }
}
