package com.ardaslegends.domain.war.rebellion;

import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * @author <a href="mailto:luktronic@gmx.at">Luktronic</a>
 */
@Getter
public class RebellionOutcome {

	private final OutcomeType outcomeType;
	private final OffsetDateTime outcomeAchievedAt;

	public RebellionOutcome(OutcomeType outcomeType) {
		this.outcomeType = outcomeType;
		this.outcomeAchievedAt = OffsetDateTime.now();
	}
}
