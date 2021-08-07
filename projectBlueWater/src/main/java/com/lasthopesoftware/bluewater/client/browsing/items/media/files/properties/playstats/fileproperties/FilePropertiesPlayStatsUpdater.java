package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.fileproperties;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kotlin.Unit;

public class FilePropertiesPlayStatsUpdater implements IPlaystatsUpdate {
	private static final Logger logger = LoggerFactory.getLogger(FilePropertiesPlayStatsUpdater.class);

	private final ProvideScopedFileProperties filePropertiesProvider;
	private final ScopedFilePropertiesStorage scopedFilePropertiesStorage;

	public FilePropertiesPlayStatsUpdater(ProvideScopedFileProperties filePropertiesProvider, ScopedFilePropertiesStorage scopedFilePropertiesStorage) {
		this.filePropertiesProvider = filePropertiesProvider;
		this.scopedFilePropertiesStorage = scopedFilePropertiesStorage;
	}

	@Override
	public Promise<?> promisePlaystatsUpdate(ServiceFile serviceFile) {
		return filePropertiesProvider.promiseFileProperties(serviceFile)
			.eventually(fileProperties -> {
				try {
					final String lastPlayedServer = fileProperties.get(KnownFileProperties.LAST_PLAYED);
					final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

					final long currentTime = System.currentTimeMillis();
					if (lastPlayedServer != null && (currentTime - duration) <= Long.valueOf(lastPlayedServer) * 1000)
						return Promise.empty();

					final String numberPlaysString = fileProperties.get(KnownFileProperties.NUMBER_PLAYS);

					int numberPlays = 0;
					if (numberPlaysString != null && !numberPlaysString.isEmpty())
						numberPlays = Integer.parseInt(numberPlaysString);

					final Promise<Unit> numberPlaysUpdate = scopedFilePropertiesStorage.promiseFileUpdate(serviceFile, KnownFileProperties.NUMBER_PLAYS, String.valueOf(++numberPlays), false);

					final String newLastPlayed = String.valueOf(currentTime / 1000);
					final Promise<Unit> lastPlayedUpdate = scopedFilePropertiesStorage.promiseFileUpdate(serviceFile, KnownFileProperties.LAST_PLAYED, newLastPlayed, false);

					return Promise.whenAll(numberPlaysUpdate, lastPlayedUpdate);
				} catch (NumberFormatException ne) {
					logger.error(ne.toString(), ne);
				}

				return Promise.empty();
			});
	}
}
