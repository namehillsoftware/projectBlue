package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile;

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayedFilePlayStatsUpdater implements IPlaystatsUpdate {
	private static final Logger logger = LoggerFactory.getLogger(PlayedFilePlayStatsUpdater.class);

	private final IConnectionProvider connectionProvider;

	public PlayedFilePlayStatsUpdater(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Promise<?> promisePlaystatsUpdate(ServiceFile serviceFile) {
		return connectionProvider.promiseResponse("File/Played", "File=" + serviceFile.getKey(), "FileType=Key")
			.then(response -> {
				try {
					final int responseCode = response.code();
					logger.info("api/v1/File/Played responded with a response code of " + responseCode);

					if (responseCode < 200 || responseCode >= 300)
						throw new HttpResponseException(responseCode);

					return null;
				} finally {
					if (response.body() != null)
						response.body().close();
				}
			});
	}
}
