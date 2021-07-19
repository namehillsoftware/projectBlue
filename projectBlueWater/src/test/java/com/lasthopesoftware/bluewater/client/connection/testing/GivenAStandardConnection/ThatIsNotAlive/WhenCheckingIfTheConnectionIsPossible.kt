package com.lasthopesoftware.bluewater.client.connection.testing.GivenAStandardConnection.ThatIsNotAlive;

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenCheckingIfTheConnectionIsPossible {

	private static boolean result;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final ConnectionTester connectionTester = new ConnectionTester();
		final FakeConnectionProvider connectionProvider = new FakeConnectionProvider();
		connectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(200,
			("<Response Status=\"NOT-OK\">" +
				"<Item Name=\"Master\">1192</Item>" +
				"<Item Name=\"Sync\">1192</Item>" +
				"<Item Name=\"LibraryStartup\">1501430846</Item>" +
			"</Response>").getBytes()), "Alive");
		result = new FuturePromise<>(connectionTester.promiseIsConnectionPossible(connectionProvider)).get();
	}

	@Test
	public void thenTheResultIsCorrect() {
		assertThat(result).isFalse();
	}
}
