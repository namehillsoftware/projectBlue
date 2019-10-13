package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsRejected.AndTheRejectionIsNeverHandled;

import com.namehillsoftware.handoff.Rejections;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.PromiseTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheRejectionIsCalled extends PromiseTestBase {

	private static final Exception thrownException = new Exception();
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		Rejections.setUnhandledRejectionsReceiver(r -> caughtException = r);
		new Promise<String>(thrownException);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}
}
