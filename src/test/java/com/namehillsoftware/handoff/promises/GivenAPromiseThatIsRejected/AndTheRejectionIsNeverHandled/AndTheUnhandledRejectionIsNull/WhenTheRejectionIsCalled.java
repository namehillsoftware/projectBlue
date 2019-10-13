package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsRejected.AndTheRejectionIsNeverHandled.AndTheUnhandledRejectionIsNull;

import com.namehillsoftware.handoff.Rejections;
import com.namehillsoftware.handoff.promises.PromiseTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheRejectionIsCalled extends PromiseTestBase {

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
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isInstanceOf(NullPointerException.class);
	}
}
