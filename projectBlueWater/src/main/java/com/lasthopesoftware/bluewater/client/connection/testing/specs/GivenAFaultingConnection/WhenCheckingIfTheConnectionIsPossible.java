package com.lasthopesoftware.bluewater.client.connection.testing.specs.GivenAFaultingConnection;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenCheckingIfTheConnectionIsPossible {

	private static boolean result;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final ConnectionTester connectionTester = new ConnectionTester();
		final FakeConnectionProvider connectionProvider = new FakeConnectionProvider();
		connectionProvider.mapResponse(p -> { throw new IOException(); }, "Alive");
		result = new FuturePromise<>(connectionTester.promiseIsConnectionPossible(connectionProvider)).get();
	}

	@Test
	public void thenTheResultIsCorrect() {
		assertThat(result).isFalse();
	}
}
