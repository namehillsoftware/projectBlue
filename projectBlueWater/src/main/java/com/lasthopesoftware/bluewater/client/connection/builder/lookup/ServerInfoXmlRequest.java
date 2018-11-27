package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import android.os.AsyncTask;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import xmlwise.XmlElement;
import xmlwise.Xmlwise;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerInfoXmlRequest implements RequestServerInfoXml {

	private final long timeout;

	public ServerInfoXmlRequest(Duration timeout) {
		this.timeout = timeout.getMillis();
	}

	@Override
	public Promise<XmlElement> promiseServerInfoXml(Library library) {
		return new QueuedPromise<>(() -> {
			final HttpURLConnection conn = (HttpURLConnection) (new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + library.getAccessCode())).openConnection();

			conn.setConnectTimeout((int) timeout);
			try {
				try (InputStream is = conn.getInputStream()) {
					return Xmlwise.createXml(IOUtils.toString(is));
				}
			} finally {
				conn.disconnect();
			}
		}, AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
