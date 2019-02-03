package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolves.AndContinuingWithResponseAndRejection.AndTheRejectionThrowsAnError;

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
		new Promise<>("test")
				.<Integer>then(result -> {
					nextReturningPromiseResult = 330 + result.hashCode();
					throw new Exception();
				}, err -> {
					caughtException = err;
					throw new Exception();
				})
				.then(nextResult -> nextReturningPromiseResult = nextResult)
				.excuse(err -> isCalled = true);
	}

	@Test
	public void thenTheRejectionIsNotCalled() {
		assertThat(caughtException).isNull();
	}

	@Test
	public void thenTheNextActionIsCalled() {
		assertThat(nextReturningPromiseResult).isEqualTo(330 + "test".hashCode());
	}

	@Test
	public void thenTheErrorIsCalled() {
		assertThat(isCalled).isTrue();
	}
}
