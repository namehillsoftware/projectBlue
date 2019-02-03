package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsRejected.AndContinuingWithResponseAndRejection;

import com.namehillsoftware.handoff.promises.Promise;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 10/17/16.
 */
public class WhenAnotherReturningPromiseIsExpected {

	private Integer nextReturningPromiseResult;
	private boolean isCalled;

	private static final Exception thrownException = new Exception();
	private static Throwable caughtException;

	@Before
	public void before() {
		new Promise<>(thrownException)
				.then(result -> 330 + result.hashCode(), err -> {
					caughtException = err;
					return 5;
				})
				.then(nextResult -> nextReturningPromiseResult = nextResult)
				.excuse(err -> isCalled = true);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}

	@Test
	public void thenTheNextActionIsCalled() {
		assertThat(nextReturningPromiseResult.intValue()).isEqualTo(5);
	}

	@Test
	public void thenTheErrorIsNotCalled() {
		assertThat(isCalled).isFalse();
	}
}
