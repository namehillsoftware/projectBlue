package com.lasthopesoftware.promises.GivenAPromiseThatIsRejected;

import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheRejectionIsCalled {

	private static Exception thrownException;
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		thrownException = new Exception();
		new Promise<String>(
				(messenger) -> messenger.sendRejection(thrownException)).error(exception -> caughtException = exception);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}
}
