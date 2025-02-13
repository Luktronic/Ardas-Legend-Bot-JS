package com.ardaslegends.presentation.api.response.movement;

import com.ardaslegends.domain.Movement;
import com.ardaslegends.presentation.api.response.army.ArmyResponse;
import com.ardaslegends.presentation.api.response.movement.path.PathResponse;
import com.ardaslegends.presentation.api.response.player.rpchar.RpCharResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
public record MovementResponse(
    Long id,
    RpCharResponse rpChar,
    ArmyResponse army,
    Boolean isCharMovement,
    List<PathResponse> path,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    Boolean isCurrentlyActive,
    Duration durationUntilComplete,
    Duration durationAlreadyMoved,
    OffsetDateTime reachesNextRegionAt,
    Duration durationUntilNextRegion,
    OffsetDateTime lastUpdatedAt

) {

    public MovementResponse(Movement movement) {
        this(
                movement.getId(),
                movement.getRpChar() == null ? null : new RpCharResponse(movement.getRpChar()),
                movement.getArmy() == null ? null : new ArmyResponse(movement.getArmy()),
                movement.getIsCharMovement(),
                movement.getPath().stream().map(PathResponse::new).toList(),
                movement.getStartTime(),
                movement.getEndTime(),
                movement.getIsCurrentlyActive(),
                movement.getDurationUntilComplete(),
                movement.getDurationAlreadyMoved(),
                movement.getReachesNextRegionAt(),
                movement.getDurationUntilNextRegion(),
                movement.getLastUpdatedAt()
        );
        log.debug("Created MovementResponse {}", this);
    }
}
