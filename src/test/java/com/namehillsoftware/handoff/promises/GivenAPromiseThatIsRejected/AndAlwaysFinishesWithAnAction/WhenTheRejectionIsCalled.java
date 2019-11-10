package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsRejected.AndAlwaysFinishesWithAnAction;

import com.namehillsoftware.handoff.promises.Promise;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheRejectionIsCalled {

	private static final Exception thrownException = new Exception();
	private static Throwable caughtException;
	private static boolean isCalled;

	@BeforeClass
	public static void before() {
		new Promise<String>(thrownException)
				.always(() -> isCalled = true)
				.excuse(exception -> caughtException = exception);
	}

	@Test
	public void thenTheAlwaysConditionIsCalled() {
		Assert.assertTrue(isCalled);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}
}
