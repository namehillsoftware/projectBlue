package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsRejected.AndTheRejectionIsNeverHandled.AndTheUnhandledRejectionIsNull;

import com.namehillsoftware.handoff.Rejections;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheRejectionIsCalled {

	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		try {
			Rejections.setUnhandledRejectionsReceiver(null);
		} catch (NullPointerException e) {
			caughtException = e;
		}
	}

	@Test
	public void thenNoExceptionIsThrown() {
		assertThat(caughtException).isNull();
	}
}
