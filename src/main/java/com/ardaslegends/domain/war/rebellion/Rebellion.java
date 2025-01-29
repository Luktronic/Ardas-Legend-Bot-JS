package com.ardaslegends.domain.war.rebellion;

import com.ardaslegends.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author <a href="mailto:luktronic@gmx.at">Luktronic</a>
 */
public class Rebellion extends AbstractEntity {

	private final String name;
	private final RebellionType type;
	private final Faction targetFaction;
	private final Faction rebellionFaction;
	private final OffsetDateTime startedAt;
	private RebellionOutcome outcome;

	public Rebellion(@Nullable String name, @NotNull RebellionType type, @NotNull InitialFaction initialFaction, @NotNull Faction targetFaction,
					 @NotNull ClaimBuild homeClaimbuild, @NotBlank String rebellionFactionName, @NotNull Player leader,
					 @NotBlank String rebellionFactionColorcode, @NotNull Long rebellionFactionRoleId,
					 @NotBlank String rebellionFactionBuffDescription, @NotNull Set<String> rebellionFactionAliases) {
		this.name = name;
		this.type = type;
		this.targetFaction = targetFaction;
		this.rebellionFaction = new Faction(rebellionFactionName, initialFaction, leader, homeClaimbuild.getRegion(),
				homeClaimbuild, rebellionFactionColorcode, rebellionFactionRoleId, rebellionFactionBuffDescription,
				new HashSet<>(rebellionFactionAliases));
		this.startedAt = OffsetDateTime.now();
		this.outcome = null;
	}

	public Optional<OffsetDateTime> getEndedAt() {
		return outcome == null ? Optional.empty() : Optional.of(outcome.getOutcomeAchievedAt());
	}

	public boolean isOver() {
		return outcome != null;
	}

	public List<Player> getMembers() {
		return Collections.unmodifiableList(rebellionFaction.getPlayers());
	}
}
