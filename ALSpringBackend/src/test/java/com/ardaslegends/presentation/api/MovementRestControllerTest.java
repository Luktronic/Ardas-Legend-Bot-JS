package com.ardaslegends.presentation.api;

import com.ardaslegends.domain.Movement;
import com.ardaslegends.domain.PathElement;
import com.ardaslegends.domain.Region;
import com.ardaslegends.domain.RegionType;
import com.ardaslegends.service.MovementService;
import com.ardaslegends.service.dto.army.MoveArmyDto;
import com.ardaslegends.service.dto.player.DiscordIdDto;
import com.ardaslegends.service.dto.player.rpchar.MoveRpCharDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class MovementRestControllerTest {
    MockMvc mockMvc;

    MovementService mockMovementService;

    MovementRestController movementRestController;

    private Region region;
    private Region region2;
    private PathElement pathElement;
    private PathElement pathElement2;
    private List<PathElement> path;


    @BeforeEach
    void setup() {
        mockMovementService = mock(MovementService.class);
        movementRestController = new MovementRestController(mockMovementService);
        mockMvc = MockMvcBuilders.standaloneSetup(movementRestController).build();

        region = Region.builder().id("91").neighboringRegions(Set.of()).regionType(RegionType.LAND).build();
        region2 = Region.builder().id("92").neighboringRegions(Set.of()).regionType(RegionType.LAND).build();
        pathElement = PathElement.builder().region(region).actualCost(2).build();
        pathElement2 = PathElement.builder().region(region2).actualCost(2).build();
        path = List.of(pathElement, pathElement2);
    }

    @Test
    void ensureCreateRoleplayCharacterMovementWorksCorrectly() throws Exception {
        log.debug("Testing if createRoleplayCharacterMovement");

        log.trace("Initializing Dto");
        MoveRpCharDto dto = new MoveRpCharDto("RandoId","12.S");

        log.trace("Initialize return movement");
        Movement movement = Movement.builder().path(path).build();

        log.trace("Initializing mock methods");
        when(mockMovementService.createRpCharMovement(dto)).thenReturn(movement);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();

        String requestJson = ow.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("http://localhost:8080/api/movement/move-char")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void ensureCancelRoleplayCharacterMoveWorksProperly() throws Exception {
        log.debug("Testing if cancelRoleplayCharacterMovement works");

        log.trace("Initializing Dto");
        DiscordIdDto dto  = new DiscordIdDto("RandoId");

        log.trace("Initialize return movement");
        Movement movement = Movement.builder().path(path).build();

        log.trace("Initializing mock methods");
        when(mockMovementService.cancelRpCharMovement(dto)).thenReturn(movement);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();

        String requestJson = ow.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders
                        .patch("http://localhost:8080/api/movement/cancel-char-move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void ensureCreateArmyMovementWorksCorrectly() throws Exception {
        log.debug("Testing if createArmyMovement works");

        log.trace("Initializing Dto");
        MoveArmyDto dto = new MoveArmyDto("1234","Knights of Gondor", "92");

        log.trace("Initialize return movement");
        Movement movement = Movement.builder().path(path).build();

        log.trace("Initializing mock methods");
        when(mockMovementService.createArmyMovement(dto)).thenReturn(movement);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();

        String requestJson = ow.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("http://localhost:8080/api/movement/move-army-or-company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void ensureCancelArmyMovementWorksCorrectly() throws Exception {
        log.debug("Testing if cancelArmyMovement works");

        log.trace("Initializing Dto");
        MoveArmyDto dto = new MoveArmyDto("1234","Knights of Gondor", null);

        log.trace("Initialize return movement");
        Movement movement = Movement.builder().path(path).build();

        log.trace("Initializing mock methods");
        when(mockMovementService.cancelArmyMovement(dto)).thenReturn(movement);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();

        String requestJson = ow.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders
                        .patch("http://localhost:8080/api/movement/cancel-army-move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }
}
