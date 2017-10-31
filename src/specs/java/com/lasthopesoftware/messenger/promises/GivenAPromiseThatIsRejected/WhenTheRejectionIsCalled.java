package com.lasthopesoftware.messenger.promises.GivenAPromiseThatIsRejected;

import com.lasthopesoftware.messenger.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheRejectionIsCalled {

	private static final Exception thrownException = new Exception();
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		new Promise<String>(thrownException).excuse(exception -> caughtException = exception);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}
}
