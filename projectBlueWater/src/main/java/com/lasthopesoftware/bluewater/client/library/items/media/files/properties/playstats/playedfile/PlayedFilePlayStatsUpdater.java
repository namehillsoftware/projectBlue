package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException;
import com.lasthopesoftware.providers.AbstractProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

public class PlayedFilePlayStatsUpdater implements IPlaystatsUpdate {
	private static final Logger logger = LoggerFactory.getLogger(PlayedFilePlayStatsUpdater.class);

	private IConnectionProvider connectionProvider;

	public PlayedFilePlayStatsUpdater(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Promise<?> promisePlaystatsUpdate(ServiceFile serviceFile) {
		return new QueuedPromise<>(() -> {
			final HttpURLConnection playedConnection = connectionProvider.getConnection("File/Played", "File=" + serviceFile.getKey(), "FileType=Key");
			try {
				final int responseCode = playedConnection.getResponseCode();
				logger.info("api/v1/File/Played responded with a response code of " + responseCode);

				if (responseCode < 200 || responseCode >= 300)
					throw new HttpResponseException(responseCode);
			} finally {
				playedConnection.disconnect();
			}

			return null;
		}, AbstractProvider.providerExecutor);
	}
}
