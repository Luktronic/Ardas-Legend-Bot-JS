package com.ardaslegends.presentation.api;

import com.ardaslegends.domain.Army;
import com.ardaslegends.domain.ClaimBuild;
import com.ardaslegends.domain.ClaimBuildType;
import com.ardaslegends.domain.SpecialBuilding;
import com.ardaslegends.presentation.AbstractRestController;
import com.ardaslegends.presentation.api.response.claimbuild.ClaimbuildResponse;
import com.ardaslegends.service.ClaimBuildService;
import com.ardaslegends.service.dto.claimbuild.CreateClaimBuildDto;
import com.ardaslegends.service.dto.claimbuilds.DeleteClaimbuildDto;
import com.ardaslegends.service.dto.claimbuilds.UpdateClaimbuildOwnerDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor

@Slf4j
@RestController
@RequestMapping(ClaimbuildRestController.BASE_URL)
public class ClaimbuildRestController extends AbstractRestController {
    public static final String BASE_URL = "/api/claimbuild";
    public static final String NAME = "/name";
    public static final String FACTION = "/faction";
    public static final String GET_TYPES = "/types";
    public static final String GET_SPECIAL_BUILDINGS = "/specialbuildings";
    public static final String PATH_CREATE_CLAIMBUILD = "/create";
    public static final String PATH_UPDATE_CLAIMBUILD = "/update";
    private static final String UPDATE_CLAIMBUILD_FATION = "/update/claimbuild-faction";
    private static final String DELETE_CLAIMBUILD = "/delete";

    private final ClaimBuildService claimBuildService;

    @GetMapping(GET_TYPES)
    public ResponseEntity<String[]> getTypes() {
        log.debug("Incoming getClaimbuildTypes Request");

        val claimbuildTypesStringArray = Arrays.stream(ClaimBuildType.values())
                .map(ClaimBuildType::getName)
                .toArray(String[]::new);

        return ResponseEntity.ok(claimbuildTypesStringArray);
    }

    @Operation(summary = "Get Claimbuilds Paginated", description = "Retrieves a Page with a set of elements, parameters define the size, which Page you want and how its sorted")
    @GetMapping
    public ResponseEntity<Page<ClaimbuildResponse>> getClaimbuildsPaginated(Pageable pageable) {
        log.debug("Incoming getClaimbuildsPaginated Request, paginated data [{}]", pageable);

        Page<ClaimBuild> pageDomain = claimBuildService.getClaimbuildsPaginated(pageable);
        var pageResponse = pageDomain.map(ClaimbuildResponse::new);

        return ResponseEntity.ok(pageResponse);
    }

    @Operation(summary = "Get Claimbuilds By Name", description = "Returns an array of claimbuilds with the specified names")
    @GetMapping(NAME)
    public ResponseEntity<ClaimbuildResponse[]> getClaimbuildsByNames(@RequestParam(name = "name") String[] names) {
        log.debug("Incoming getClaimbuildsByName Request, parameter names: [{}]", (Object) names);

        List<ClaimBuild> claimBuilds = claimBuildService.getClaimBuildsByNames(names);

        log.debug("Building ClaimbuildResponses with claimbuilds [{}]", claimBuilds);
        ClaimbuildResponse[] response = claimBuilds.stream().map(ClaimbuildResponse::new).toArray(ClaimbuildResponse[]::new);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Claimbuilds By Faction", description = "Returns an array of claimbuilds of the specified faction")
    @GetMapping(FACTION)
    public ResponseEntity<ClaimbuildResponse[]> getClaimbuildsByFaction(@RequestParam String faction) {
        log.debug("Incoming getClaimbuildsByFaction Request, parameter faction: [{}]", faction);

        List<ClaimBuild> claimBuilds = claimBuildService.getClaimBuildsByFaction(faction);

        log.debug("Building ClaimbuildResponses with claimbuilds [{}]", claimBuilds);
        ClaimbuildResponse[] response = claimBuilds.stream().map(ClaimbuildResponse::new).toArray(ClaimbuildResponse[]::new);

        return ResponseEntity.ok(response);
    }

    @GetMapping(GET_SPECIAL_BUILDINGS)
    public HttpEntity<String[]> getSpecialBuildings() {
        log.debug("Incoming getAllSpecialBuilds request");

        val specialBuildingsStringArray = Arrays.stream(SpecialBuilding.values())
                .map(SpecialBuilding::getName)
                .toArray(String[]::new);

        return ResponseEntity.ok(specialBuildingsStringArray);
    }

    @PostMapping(PATH_CREATE_CLAIMBUILD)
    public HttpEntity<ClaimbuildResponse> createClaimbuild(@RequestBody CreateClaimBuildDto dto) {
        log.debug("Incoming createClaimbuild Request: Data [{}]", dto);

        log.debug("Calling claimBuildService.createClaimbuild");
        ClaimBuild claimBuild = claimBuildService.createClaimbuild(dto, true);

        val response = new ClaimbuildResponse(claimBuild);

        log.info("Sending successful createClaimbuild Request for [{}]", claimBuild.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(PATH_UPDATE_CLAIMBUILD)
    public HttpEntity<ClaimbuildResponse> updateClaimbuild(@RequestBody CreateClaimBuildDto dto) {
        log.debug("Incoming updateClaimbuild Request: Data [{}]", dto);

        log.debug("Calling claimBuildService.createClaimbuild");
        ClaimBuild claimBuild = claimBuildService.createClaimbuild(dto, false);

        val response = new ClaimbuildResponse(claimBuild);

        log.info("Sending successful updateClaimbuild Request for [{}]", claimBuild.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(UPDATE_CLAIMBUILD_FATION)
    public HttpEntity<UpdateClaimbuildOwnerDto> updateClaimbuildOwner(@RequestBody UpdateClaimbuildOwnerDto dto) {
        log.debug("Incoming update Claimbuild Owner Request with data [{}]", dto);

        log.trace("Calling wrappedServiceExecution of setOwnerFaction");
        var result = claimBuildService.changeOwnerFromDto(dto);

        log.trace("Building response Dto");
        UpdateClaimbuildOwnerDto response = new UpdateClaimbuildOwnerDto(result.getName(), result.getOwnedBy().getName());

        log.info("Sending successful response [{}] to bot!", response);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(DELETE_CLAIMBUILD)
    public HttpEntity<DeleteClaimbuildDto> deleteClaimbuild(@RequestBody DeleteClaimbuildDto dto) {
        log.debug("Incoming delete Claimbuild Request with data [{}]", dto);

        log.trace("Calling wrappedServiceExecution of deleteClaimbuild");
        var result = claimBuildService.deleteClaimbuild(dto);


        log.trace("Building body Dto");
        DeleteClaimbuildDto body = new DeleteClaimbuildDto(result.getName(),
                 result.getStationedArmies().stream().map(Army::getName).collect(Collectors.toList()),
                 result.getCreatedArmies().stream().map(Army::getName).collect(Collectors.toList()));

        log.info("Creating response with body [{}]", body);
        var response = ResponseEntity.ok(body);
        return response;
    }
}
