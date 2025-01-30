package com.ardaslegends.domain.exception.war.rebellion;

import com.ardaslegends.domain.exception.IllegalDomainActionException;
import com.ardaslegends.domain.war.rebellion.Rebellion;
import jakarta.validation.constraints.NotNull;
import lombok.val;

/**
 * @author <a href="mailto:luktronic@gmx.at">Luktronic</a>
 */
public class IllegalRebellionActionException extends IllegalDomainActionException {

	private static final String ALREADY_ENDED = "Rebellion '%s' has already ended on %s!";

	public static IllegalRebellionActionException rebellionAlreadyEnded(@NotNull Rebellion rebellion) {
		val endedAt = rebellion.getEndedAt().orElseThrow(() ->
			new IllegalArgumentException("IllegalRebellionActionException: received Rebellion without outcome!"));
		return new IllegalRebellionActionException(ALREADY_ENDED.formatted(rebellion.getName(), endedAt));
	}

	private IllegalRebellionActionException(String message) {
		super(message);
	}
}
