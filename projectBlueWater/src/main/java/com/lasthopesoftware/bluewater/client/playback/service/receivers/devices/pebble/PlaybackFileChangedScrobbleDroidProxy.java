package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

class PlaybackFileChangedScrobbleDroidProxy extends BroadcastReceiver {

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
			.next(runCarelessly(fileProperties -> {
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
