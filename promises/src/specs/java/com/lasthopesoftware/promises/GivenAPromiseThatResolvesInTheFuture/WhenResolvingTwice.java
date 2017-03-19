package specs.GivenAPromiseThatResolvesInTheFuture;

import com.lasthopesoftware.promises.Promise;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by david on 2/20/17.
 */

public class WhenResolvingTwice {

	private static Object expectedResult = new Object();
	private static Object unexpectedResult = new Object();
	private static Object result;

	@BeforeClass
	public static void before() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(2);

		new Promise<>((resolve, reject) ->  new Thread(() -> {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			resolve.withResult(expectedResult);
			latch.countDown();
			resolve.withResult(unexpectedResult);
			latch.countDown();
		}).start())
		.then(result -> WhenResolvingTwice.result = result);;

		latch.await(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheExpectedResultIsPresent() {
		Assert.assertEquals(expectedResult, result);
	}
}
