package com.lasthopesoftware.promises.specs.GivenAPromiseThatIsRejected;

import com.lasthopesoftware.promises.Promise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheRejectionIsCalled {

	private Exception thrownException;
	private Exception caughtException;

	@Before
	public void before() {
		thrownException = new Exception();
		new Promise<String>((resolve, reject) -> reject.run(thrownException))
				.error(exception -> caughtException = exception);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
