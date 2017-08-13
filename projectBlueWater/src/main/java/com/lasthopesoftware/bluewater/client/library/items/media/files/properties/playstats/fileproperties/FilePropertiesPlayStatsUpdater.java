package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.IFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.messenger.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilePropertiesPlayStatsUpdater implements IPlaystatsUpdate {
	private static final Logger logger = LoggerFactory.getLogger(FilePropertiesPlayStatsUpdater.class);

	private final IFilePropertiesProvider filePropertiesProvider;
	private final IConnectionProvider connectionProvider;

	public FilePropertiesPlayStatsUpdater(IConnectionProvider connectionProvider, IFilePropertiesProvider filePropertiesProvider) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesProvider = filePropertiesProvider;
	}

	@Override
	public Promise<?> promisePlaystatsUpdate(ServiceFile serviceFile) {
		final int fileKey = serviceFile.getKey();
		return filePropertiesProvider.promiseFileProperties(fileKey)
			.then(fileProperties -> {
				try {
					final String lastPlayedServer = fileProperties.get(FilePropertiesProvider.LAST_PLAYED);
					final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

					final long currentTime = System.currentTimeMillis();
					if (lastPlayedServer != null && (currentTime - duration) <= Long.valueOf(lastPlayedServer) * 1000)
						return null;

					final String numberPlaysString = fileProperties.get(FilePropertiesProvider.NUMBER_PLAYS);

					int numberPlays = 0;
					if (numberPlaysString != null && !numberPlaysString.isEmpty())
						numberPlays = Integer.parseInt(numberPlaysString);

					FilePropertiesStorage.storeFileProperty(connectionProvider, fileKey, FilePropertiesProvider.NUMBER_PLAYS, String.valueOf(++numberPlays), false);

					final String newLastPlayed = String.valueOf(currentTime / 1000);
					FilePropertiesStorage.storeFileProperty(connectionProvider, fileKey, FilePropertiesProvider.LAST_PLAYED, newLastPlayed, false);
				} catch (NumberFormatException ne) {
					logger.error(ne.toString(), ne);
				}

				return null;
			});
	}
}
