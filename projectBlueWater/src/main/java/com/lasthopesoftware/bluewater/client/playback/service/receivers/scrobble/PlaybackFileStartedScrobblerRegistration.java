package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

import java.util.Collection;
import java.util.Collections;

import static com.lasthopesoftware.messenger.promises.response.ImmediateAction.perform;

public class PlaybackFileStartedScrobblerRegistration implements IConnectionDependentReceiverRegistration {

	private static final Collection<IntentFilter> intents = Collections.singleton(new IntentFilter(PlaylistEvents.onFileStart));

	@Override
	public BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider) {
		final CachedFilePropertiesProvider filePropertiesProvider =
			new CachedFilePropertiesProvider(
				connectionProvider,
				FilePropertyCache.getInstance(),
				new FilePropertiesProvider(
					connectionProvider,
					FilePropertyCache.getInstance()));

		return new PlaybackFileChangedScrobbleDroidProxy(filePropertiesProvider, ScrobbleIntentProvider.getInstance());
	}

	@Override
	public Collection<IntentFilter> forIntents() {
		return intents;
	}

	private static class PlaybackFileChangedScrobbleDroidProxy extends BroadcastReceiver {

		private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
		private final ScrobbleIntentProvider scrobbleIntentProvider;

		public PlaybackFileChangedScrobbleDroidProxy(CachedFilePropertiesProvider cachedFilePropertiesProvider, ScrobbleIntentProvider scrobbleIntentProvider) {
			this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
			this.scrobbleIntentProvider = scrobbleIntentProvider;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
			if (fileKey < 0) return;

			cachedFilePropertiesProvider
				.promiseFileProperties(fileKey)
				.then(perform(fileProperties -> {
					final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
					final String name = fileProperties.get(FilePropertiesProvider.NAME);
					final String album = fileProperties.get(FilePropertiesProvider.ALBUM);
					final long duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);
					final String trackNumberString = fileProperties.get(FilePropertiesProvider.TRACK);
					final Integer trackNumber = trackNumberString != null && !trackNumberString.isEmpty() ? Integer.valueOf(trackNumberString) : null;

					final Intent scrobbleDroidIntent = scrobbleIntentProvider.provideScrobbleIntent(true);
					scrobbleDroidIntent.putExtra("artist", artist);
					scrobbleDroidIntent.putExtra("album", album);
					scrobbleDroidIntent.putExtra("track", name);
					scrobbleDroidIntent.putExtra("secs", (int) (duration / 1000));
					if (trackNumber != null)
						scrobbleDroidIntent.putExtra("tracknumber", trackNumber.intValue());

					context.sendBroadcast(scrobbleDroidIntent);
				}));
		}
	}
}
