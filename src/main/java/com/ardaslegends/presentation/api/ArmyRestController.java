package com.ardaslegends.presentation.api;

import com.ardaslegends.domain.Army;
import com.ardaslegends.presentation.AbstractRestController;
import com.ardaslegends.presentation.api.response.army.ArmyResponse;
import com.ardaslegends.service.ArmyService;
import com.ardaslegends.service.dto.army.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor

@Slf4j
@RestController
@RequestMapping(ArmyRestController.BASE_URL)
public class ArmyRestController extends AbstractRestController {
    public static final String BASE_URL = "/api/army";
    private static final String PATH_CREATE_ARMY = "/create-army";
    private static final String PATH_BIND_ARMY = "/bind";
    private static final String PATH_UNBIND_ARMY = "/unbind";
    private static final String PATH_DISBAND_ARMY = "/disband";
    private static final String PATH_DELETE_ARMY = "/delete";
    private static final String PATH_START_HEALING = "/heal-start";
    private static final String PATH_STOP_HEALING = "/heal-stop";
    private static final String PATH_STATION = "/station";
    private static final String PATH_UNSTATION = "/unstation";
    private static final String PATH_SET_FREE_TOKENS = "/set-free-tokens";
    private static final String PATH_PICK_SIEGE = "/pick-siege";
    private static final String PATH_UPKEEP = "/upkeep";
    private static final String PATH_UPKEEP_PER_FACTION = "/upkeep/{faction}";
    private static final String PATH_SET_IS_PAID = "/setPaid";
    private static final String PATH_GET_UNPAID =  "/unpaid";

    private final ArmyService armyService;

    @Operation(summary = "Get Armies Paginated", description = "Retrieves a Page with a set of elements, parameters define the size, which Page you want and how its sorted")
    @GetMapping
    public HttpEntity<Page<ArmyResponse>> getArmiesPaginated(Pageable pageable) {
        log.debug("Incoming getArmiesPaginated: Data [{}]", pageable.toString());

        Page<Army> pageDomain = armyService.getArmiesPaginated(pageable);
        Page<ArmyResponse> pageResponse = pageDomain.map(ArmyResponse::new);

        return ResponseEntity.ok(pageResponse);
    }
    @PostMapping(PATH_CREATE_ARMY)
    public HttpEntity<ArmyResponse> createArmy(@RequestBody CreateArmyDto dto) {
        log.debug("Incoming createArmy Request: Data [{}]", dto);

        if(dto.unitString() != null) {
            log.debug("Found unitString in CreateArmyDto, building units from string");
            var units = armyService.convertUnitInputIntoUnits(dto.unitString());
            dto = new CreateArmyDto(dto.executorDiscordId(), dto.name(), dto.armyType(), dto.claimBuildName(), units);
        }
        log.debug("Calling ArmyService.createArmy");
        Army createdArmy = armyService.createArmy(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(createdArmy);

        log.info("Sending successful createArmy Request for [{}]", createdArmy.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_BIND_ARMY)
    public HttpEntity<ArmyResponse> bindArmy(@RequestBody BindArmyDto dto) {
        log.debug("Incoming bindArmy Request: Data [{}]", dto);

        log.debug("Calling ArmyService.bind()");
        Army boundArmy = armyService.bind(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(boundArmy);

        log.info("Sending successful bindArmy request for [{}]", boundArmy.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_UNBIND_ARMY)
    public HttpEntity<ArmyResponse> unbindArmy(@RequestBody BindArmyDto dto) {
        log.debug("Incoming unbindArmy Request: Data [{}]", dto);

        log.debug("Calling ArmyService.unbind()");
        Army unboundArmy = armyService.unbind(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(unboundArmy);

        log.info("Sending successful unbindArmy request for [{}]", unboundArmy.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(PATH_DISBAND_ARMY)
    public HttpEntity<ArmyResponse> disbandArmy(@RequestBody DeleteArmyDto dto) {
        log.debug("Incoming disbandArmy Request: Data [{}]", dto);

        log.debug("Calling ArmyService.unbind()");
        Army disbandedArmy = armyService.disbandFromDto(dto, false);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(disbandedArmy);

        log.info("Sending successful disbandArmy request for [{}]", disbandedArmy.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(PATH_DELETE_ARMY)
    public HttpEntity<ArmyResponse> deleteArmy(@RequestBody DeleteArmyDto dto) {
        log.debug("Incoming deleteArmy Request: Data [{}]", dto);

        log.debug("Calling ArmyService.disband()");
        Army deletedArmy = armyService.disbandFromDto(dto, true);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(deletedArmy);

        log.info("Sending successful deleteArmy request for [{}]", deletedArmy.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_START_HEALING)
    public HttpEntity<ArmyResponse> startHealing(@RequestBody UpdateArmyDto dto) {
        log.debug("Incoming start healing Request: Data [{}]", dto);

        log.debug("Calling healStart()");
        Army modifiedArmy = armyService.healStart(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(modifiedArmy);

        log.info("Sending successful start healing Request for [{}]", modifiedArmy.toString());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_STOP_HEALING)
    public HttpEntity<ArmyResponse> stopHealing(@RequestBody UpdateArmyDto dto) {
        log.debug("Incoming stop healing Request: Data [{}]", dto);

        log.debug("Calling healStop()");
        Army modifiedArmy = armyService.healStop(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(modifiedArmy);

        log.info("Sending successful stop healing Request for [{}]", modifiedArmy.toString());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_STATION)
    public HttpEntity<ArmyResponse> station(@RequestBody StationDto dto) {
        log.debug("Incoming station request: Data [{}]", dto);

        log.debug("Calling station()");
        Army modifiedArmy = armyService.station(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(modifiedArmy);

        log.info("Sending successful station request for [{}]", modifiedArmy.toString());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_UNSTATION)
    public HttpEntity<ArmyResponse> unstation(@RequestBody UnstationDto dto) {
        log.debug("Incoming station request: Data [{}]", dto);

        log.debug("Calling unstation()");
        Army modifiedArmy = armyService.unstation(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(modifiedArmy);

        log.info("Sending successful unstation request for [{}]", modifiedArmy.toString());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_SET_FREE_TOKENS)
    public HttpEntity<ArmyResponse> setFreeArmyTokens(@RequestBody UpdateArmyDto dto) {
        log.debug("Incoming setFreeArmyTokens Request: Data [{}]", dto);

        log.debug("Calling ArmyService.setFreeArmyTokens()");
        Army deletedArmy = armyService.setFreeArmyTokens(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(deletedArmy);

        log.info("Sending successful setFreeArmyTokens request for [{}]", deletedArmy.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_PICK_SIEGE)
    public HttpEntity<ArmyResponse> pickSiege(@RequestBody PickSiegeDto dto) {
        log.debug("Incoming pickSiege Request: Data [{}]", dto);

        log.debug("Calling ArmyService.pickSiege()");
        Army army = armyService.pickSiege(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(army);

        log.info("Sending successful pickSiege request for [{}]", army.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping(PATH_UPKEEP)
    public HttpEntity<List<UpkeepDto>> upkeep() {
        log.debug("Incoming upkeep request");

        log.debug("Calling ArmyService.upkeep()");
        var upkeepList = armyService.upkeep();

        log.info("Sending successful upkeep request");
        return ResponseEntity.ok(upkeepList);
    }

    @GetMapping(PATH_UPKEEP_PER_FACTION)
    public HttpEntity<UpkeepDto> upkeepPerFaction(@PathVariable("faction") String factionName) {
        log.debug("Incoming upkeep per faction request for faction: [{}]", factionName);

        log.debug("Calling ArmyService.upkeepPerFaction()");
        UpkeepDto result = armyService.getUpkeepOfFaction(factionName);

        log.info("Sending successful upkeepPerFaction Request for faction: [{}]", factionName);
        return ResponseEntity.ok(result);
    }

    @PatchMapping(PATH_SET_IS_PAID)
    public HttpEntity<ArmyResponse> setIsPaid(@RequestBody UpdateArmyDto dto) {
        log.debug("Incoming setIsPaid Request for army or company [{}]", dto);

        log.trace("Calling wrappedServiceExecution armyService.setIsPaid");
        var result = armyService.setIsPaid(dto);
        log.debug("Converting to ArmyResponse");
        ArmyResponse response = new ArmyResponse(result);

        log.info("Sending setPaid Response, success [{}]", result);
        return ResponseEntity.ok(response);
    }

    @GetMapping(PATH_GET_UNPAID)
    public HttpEntity<List<ArmyResponse>> getUnpaid() {
        log.debug("Incoming getUnpaid Request");

        log.trace("Calling wrappedServiceExecution, armyService.getUnpaid");
        var result = armyService.getUnpaid();
        log.debug("Converting to ArmyResponse");
        var response = result.stream().map(ArmyResponse::new).toList();

        log.info("Sending getUnpaid Response, data [{}]", response);
        return ResponseEntity.ok(response);
    }
}
