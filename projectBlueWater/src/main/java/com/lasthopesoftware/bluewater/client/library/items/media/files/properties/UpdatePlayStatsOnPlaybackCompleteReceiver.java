package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 3/5/16.
 */
public class UpdatePlayStatsOnPlaybackCompleteReceiver extends BroadcastReceiver {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePlayStatsOnPlaybackCompleteReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		final int libraryId = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileLibraryId, -1);
		if (libraryId < 0) return;

		final int fileKey = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileKey, -1);
		if (fileKey < 0) return;

		LibrarySession.GetLibrary(context, libraryId, library -> AccessConfigurationBuilder.buildConfiguration(context, library, (task, urlProvider) -> {
			final ConnectionProvider connectionProvider = new ConnectionProvider(urlProvider);
			try {
				final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, fileKey);
				final Map<String, String> fileProperties = filePropertiesProvider.get();
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
			} catch (InterruptedException | ExecutionException e) {
				logger.warn("Updating play stats for file " + fileKey + " was interrupted or encountered an error.", e);
			} catch (NumberFormatException ne) {
				logger.error(ne.toString(), ne);
			}
		}));
	}
}
