package com.lasthopesoftware.bluewater.client.connection.testing;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.Response;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ConnectionTester implements TestConnections {

	private static final Duration stdTimeoutTime = Duration.millis(30000);

	private static final Logger mLogger = LoggerFactory.getLogger(ConnectionTester.class);

	@Override
	public Promise<Boolean> promiseIsConnectionPossible(IConnectionProvider connectionProvider) {
		return connectionProvider.promiseResponse("Alive").then(this::doTestSynchronously);
	}

	private boolean doTestSynchronously(Response response) {
			try {
				final InputStream is = response.body().byteStream();
				try {
					final StandardRequest responseDao = StandardRequest.fromInputStream(is);

					return responseDao != null && responseDao.isStatus();
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						mLogger.error("Error closing connection, device failure?", e);
					}
				}
			} catch (IllegalArgumentException e) {
				mLogger.warn("Illegal argument passed in", e);
			}

		return false;
	}
}
