package com.ardaslegends.presentation.api;

import com.ardaslegends.domain.war.battle.Battle;
import com.ardaslegends.presentation.api.response.war.BattleResponse;
import com.ardaslegends.service.dto.war.battle.ConcludeBattleDto;
import com.ardaslegends.service.dto.war.battle.CreateBattleDto;
import com.ardaslegends.service.war.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor

@Slf4j
@RestController
@Tag(name = "Battle Controller", description = "REST endpoints concerning Battles")
@RequestMapping(BattleRestController.BASE_URL)
public class BattleRestController {
    public static final String BASE_URL = "/api/battles";
    public static final String CONCLUDE = "/conclude";

    private final BattleService battleService;

    @Operation(summary = "Declare Battle", description = "Declare a battle on an army or claimbuild")
    @PostMapping
    public ResponseEntity<BattleResponse> createBattle(@RequestBody CreateBattleDto dto) {
        log.debug("Incoming createBattle Request, dto [{}]", dto);

        log.debug("Calling battleService.createBattle");
        Battle battle = battleService.createBattle(dto);

        log.debug("Building response");
        val battleResponse = new BattleResponse(battle);

        log.info("Sending successful createBattle response [{}]", battleResponse);
        return ResponseEntity.ok(battleResponse);
    }

    @Operation(summary = "Conclude Battle", description = "Concludes a battle with the passed result")
    @PostMapping(CONCLUDE)
    public ResponseEntity<BattleResponse> concludeBattle(@RequestBody ConcludeBattleDto dto) {
        log.debug("Incoming concludeBattle Request, dto [{}]", dto);

        log.debug("Calling battleService.concludeBattle");
        Battle battle = battleService.concludeBattle(dto);

        log.debug("Building response");
        val battleResponse = new BattleResponse(battle);

        log.info("Sending successful concludeBattle response [{}]", battleResponse);
        return ResponseEntity.ok(battleResponse);
    }
}
