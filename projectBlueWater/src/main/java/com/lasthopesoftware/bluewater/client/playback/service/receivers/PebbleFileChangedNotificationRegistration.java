package com.lasthopesoftware.bluewater.client.playback.service.receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

import java.util.Collection;
import java.util.Collections;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class PebbleFileChangedNotificationRegistration implements IConnectionDependentReceiverRegistration {

	private static final Collection<IntentFilter> intents = Collections.singleton(new IntentFilter(PlaylistEvents.onPlaylistChange));

	@Override
	public BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider) {
		final CachedFilePropertiesProvider filePropertiesProvider =
			new CachedFilePropertiesProvider(
				connectionProvider,
				FilePropertyCache.getInstance(),
				new FilePropertiesProvider(
					connectionProvider,
					FilePropertyCache.getInstance()));

		return new PlaybackFileChangedScrobbleDroidProxy(filePropertiesProvider);
	}

	@Override
	public Collection<IntentFilter> forIntents() {
		return intents;
	}

	private static class PlaybackFileChangedScrobbleDroidProxy extends BroadcastReceiver {

		private static final String PEBBLE_NOTIFY_INTENT = "com.getpebble.action.NOW_PLAYING";

		private final CachedFilePropertiesProvider cachedFilePropertiesProvider;

		public PlaybackFileChangedScrobbleDroidProxy(CachedFilePropertiesProvider cachedFilePropertiesProvider) {
			this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
			if (fileKey < 0) return;

			cachedFilePropertiesProvider
				.promiseFileProperties(fileKey)
				.then(runCarelessly(fileProperties -> {
					final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
					final String name = fileProperties.get(FilePropertiesProvider.NAME);
					final String album = fileProperties.get(FilePropertiesProvider.ALBUM);

					final Intent pebbleIntent = new Intent(PEBBLE_NOTIFY_INTENT);
					pebbleIntent.putExtra("artist", artist);
					pebbleIntent.putExtra("album", album);
					pebbleIntent.putExtra("track", name);

					context.sendBroadcast(pebbleIntent);
				}));
		}
	}
}
