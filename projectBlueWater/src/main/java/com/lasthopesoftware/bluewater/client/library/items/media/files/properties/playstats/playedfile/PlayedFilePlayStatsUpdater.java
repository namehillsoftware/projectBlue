package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.lasthopesoftware.providers.AbstractProvider;

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
	public Promise<Boolean> promisePlaystatsUpdate(int fileKey) {
		return new QueuedPromise<>(() -> {
			final HttpURLConnection playedConnection = connectionProvider.getConnection("File/Played", "File=" + fileKey, "FileType=Key");
			try {
				final int responseCode = playedConnection.getResponseCode();
				logger.info("api/v1/File/Played responded with a response code of " + responseCode);
			} finally {
				playedConnection.disconnect();
			}

			return true;
		}, AbstractProvider.providerExecutor);
	}
}
