package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsRejected.AndEventuallyContinuesWithANullResponse;

import com.namehillsoftware.handoff.promises.Promise;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 10/17/16.
 */
public class WhenAnotherReturningPromiseIsExpected {

	private static final Exception thrownException = new Exception();
	private static final Exception otherException = new Exception();
	private static Throwable caughtException;

	@Before
	public void before() {
		new Promise<>(thrownException)
				.eventually(result -> null, err -> {
					throw otherException;
				})
				.excuse(err -> caughtException = err);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(otherException);
	}
}
