package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsRejected.AndFinishesWithAnErroneousAction;

import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheRejectionIsCalled {

	private static final Exception thrownException = new Exception();
	private static final Exception otherException = new Exception();
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		new Promise<String>(thrownException)
				.inevitably(() -> new Promise<>(otherException))
				.excuse(exception -> caughtException = exception);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(otherException);
	}
}
