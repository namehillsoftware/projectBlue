package com.lasthopesoftware.bluewater.client.connection.testing;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ConnectionTester implements TestConnections {

	private static final Logger mLogger = LoggerFactory.getLogger(ConnectionTester.class);

	@Override
	public Promise<Boolean> promiseIsConnectionPossible(IConnectionProvider connectionProvider) {
		return connectionProvider.promiseResponse("Alive").then(this::doTestSynchronously, e -> false);
	}

	private boolean doTestSynchronously(Response response) {
		final ResponseBody body = response.body();
		if (body == null) return false;

		try (final InputStream is = body.byteStream()) {
			final StandardRequest responseDao = StandardRequest.fromInputStream(is);

			return responseDao != null && responseDao.isStatus();
		} catch (IOException e) {
			mLogger.error("Error closing connection, device failure?", e);
		} catch (IllegalArgumentException e) {
			mLogger.warn("Illegal argument passed in", e);
		} finally {
			body.close();
		}

		return false;
	}
}
