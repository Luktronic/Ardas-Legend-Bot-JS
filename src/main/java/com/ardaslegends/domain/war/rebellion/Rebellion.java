package com.ardaslegends.domain.war.rebellion;

import com.ardaslegends.domain.*;
import com.ardaslegends.domain.exception.war.rebellion.IllegalRebellionActionException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author <a href="mailto:luktronic@gmx.at">Luktronic</a>
 */
@Slf4j
@Getter
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

		try(val validatorFactory = Validation.buildDefaultValidatorFactory()) {
			val validator = validatorFactory.getValidator();
			val constructor = this.getClass().getConstructor(String.class, RebellionType.class, InitialFaction.class, Faction.class,
					ClaimBuild.class, String.class, Player.class,
					String.class, Long.class,
					String.class, Set.class);
			val params = new Object[]{name, type, initialFaction, targetFaction,
				homeClaimbuild, rebellionFactionName, leader,
				rebellionFactionColorcode, rebellionFactionRoleId,
				rebellionFactionBuffDescription, rebellionFactionAliases};
			validator.forExecutables().validateConstructorParameters(constructor, params);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		this.name = name;
		this.type = type;
		this.targetFaction = targetFaction;
		this.rebellionFaction = new Faction(rebellionFactionName, initialFaction, leader, homeClaimbuild.getRegion(),
				homeClaimbuild, rebellionFactionColorcode, rebellionFactionRoleId, rebellionFactionBuffDescription,
				new HashSet<>(rebellionFactionAliases));
		this.startedAt = OffsetDateTime.now();
		this.outcome = null;
	}

	public void end(@NotNull OutcomeType outcomeType) {
		log.debug("Ending rebellion [{}]", this);
		if(this.outcome != null) {
			log.warn("Rebellion [{}] could not be ended because an outcome was already present: [{}]", this, outcome);
			throw IllegalRebellionActionException.rebellionAlreadyEnded(this);
		}

		this.outcome = new RebellionOutcome(outcomeType);
		log.debug("Created RebellionOutcome: {}", outcome);
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
