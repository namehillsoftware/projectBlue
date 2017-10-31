package com.lasthopesoftware.messenger.promises.GivenAPromiseThatIsRejected.AndEventuallyContinuingWithResponseAndRejection.AndTheRejectionThrowsAnError;

import com.lasthopesoftware.messenger.promises.Promise;
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
				.eventually(result -> new Promise<>(330 + result.hashCode()), err -> {
					caughtException = err;
					throw new Exception();
				})
				.then(nextResult -> nextReturningPromiseResult = nextResult)
				.excuse(err -> isCalled = true);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}

	@Test
	public void thenTheNextActionIsNotCalled() {
		assertThat(nextReturningPromiseResult).isNull();
	}

	@Test
	public void thenTheErrorIsCalled() {
		assertThat(isCalled).isTrue();
	}
}
