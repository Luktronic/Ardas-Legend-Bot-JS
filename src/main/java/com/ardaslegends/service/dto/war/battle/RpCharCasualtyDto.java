package com.ardaslegends.service.dto.war.battle;

import io.swagger.v3.oas.annotations.media.Schema;

public record RpCharCasualtyDto(
        @Schema(description = "Discord ID of the player that died in battle", example = "261173268365443074")
        String discordId,
        @Schema(description = "(optional) Discord ID of the player that killed the victim. Will generate a death message like: '[player] was slain by [slayer]'. " +
                "Will be prioritized over optionalCause.",
                example = "253505646190657537")
        String slainByPlayer,
        @Schema(description = "(optional) Weapon the player was slain by. Will be used with slainByPlayer to generate a message like: '[player] was slain by [slayer] using [weapon]",
                example = "a mighty sword")
        String slainByWeapon,
        @Schema(description = "(optional) Optional cause of death. Can be used to generate a custom death message: '[player] [optionalCause]'. " +
                "Will be ignored if slainByPlayer is specified.", example = "fell from the city wall")
        String optionalCause
) {
}
