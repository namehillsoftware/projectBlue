package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.lasthopesoftware.providers.AbstractProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class UpdatePlayStatsOnPlaybackCompleteReceiver extends BroadcastReceiver {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePlayStatsOnPlaybackCompleteReceiver.class);
	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesProvider filePropertiesProvider;

	public UpdatePlayStatsOnPlaybackCompleteReceiver(IConnectionProvider connectionProvider, IFilePropertiesProvider filePropertiesProvider) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesProvider = filePropertiesProvider;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int libraryId = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileLibraryId, -1);
		if (libraryId < 0) return;

		final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
		if (fileKey < 0) return;

		connectionProvider
			.promiseConnectionProgramVersion()
			.eventually(programVersion -> {
				if (programVersion != null && programVersion.major >= 22) {
					return new QueuedPromise<>(() -> {
						final HttpURLConnection playedConnection = connectionProvider.getConnection("Played", "File=" + fileKey, "FileType=Key");
						try {
							final int responseCode = playedConnection.getResponseCode();
							logger.info("api/v1/Played responded with a response code of " + responseCode);
						} finally {
							playedConnection.disconnect();
						}

						return null;
					}, AbstractProvider.providerExecutor);
				}

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
			})
			.excuse(runCarelessly(e -> logger.error("There was an error updating the playstats for the file with key " + fileKey)));
	}
}
