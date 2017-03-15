package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.vedsoft.futures.callables.VoidFunc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by david on 3/5/16.
 */
public class UpdatePlayStatsOnPlaybackCompleteReceiver extends BroadcastReceiver {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePlayStatsOnPlaybackCompleteReceiver.class);
	private final ILibraryProvider libraryProvider;
	private final IFilePropertiesProvider filePropertiesProvider;

	public UpdatePlayStatsOnPlaybackCompleteReceiver(ILibraryProvider libraryProvider, IFilePropertiesProvider filePropertiesProvider) {
		this.libraryProvider = libraryProvider;
		this.filePropertiesProvider = filePropertiesProvider;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int libraryId = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileLibraryId, -1);
		if (libraryId < 0) return;

		final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
		if (fileKey < 0) return;

		libraryProvider
			.getLibrary(libraryId)
			.then(VoidFunc.runCarelessly(library -> AccessConfigurationBuilder.buildConfiguration(context, library, (urlProvider) -> {
					this.filePropertiesProvider
						.promiseFileProperties(fileKey)
						.then(VoidFunc.runCarelessly(fileProperties -> {
							final IConnectionProvider connectionProvider = new ConnectionProvider(urlProvider);
							try {
								final String lastPlayedServer = fileProperties.get(FilePropertiesProvider.LAST_PLAYED);
								final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

								final long currentTime = System.currentTimeMillis();
								if (lastPlayedServer != null && (currentTime - duration) <= Long.valueOf(lastPlayedServer) * 1000) return;

								final String numberPlaysString = fileProperties.get(FilePropertiesProvider.NUMBER_PLAYS);

								int numberPlays = 0;
								if (numberPlaysString != null && !numberPlaysString.isEmpty())
									numberPlays = Integer.parseInt(numberPlaysString);

								FilePropertiesStorage.storeFileProperty(connectionProvider, fileKey, FilePropertiesProvider.NUMBER_PLAYS, String.valueOf(++numberPlays));

								final String newLastPlayed = String.valueOf(currentTime / 1000);
								FilePropertiesStorage.storeFileProperty(connectionProvider, fileKey, FilePropertiesProvider.LAST_PLAYED, newLastPlayed);
							} catch (NumberFormatException ne) {
								logger.error(ne.toString(), ne);
							}
						}));
			})));
	}
}
