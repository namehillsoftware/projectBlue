package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

class PebbleFileChangedProxy extends BroadcastReceiver {

	private static final String PEBBLE_NOTIFY_INTENT = "com.getpebble.action.NOW_PLAYING";

	private final CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider;

	public PebbleFileChangedProxy(CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider) {
		this.cachedSessionFilePropertiesProvider = cachedSessionFilePropertiesProvider;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
		if (fileKey < 0) return;

		cachedSessionFilePropertiesProvider
			.promiseFileProperties(new ServiceFile(fileKey))
			.then(fileProperties -> {
				final String artist = fileProperties.get(KnownFileProperties.ARTIST);
				final String name = fileProperties.get(KnownFileProperties.NAME);
				final String album = fileProperties.get(KnownFileProperties.ALBUM);

				final Intent pebbleIntent = new Intent(PEBBLE_NOTIFY_INTENT);
				pebbleIntent.putExtra("artist", artist);
				pebbleIntent.putExtra("album", album);
				pebbleIntent.putExtra("track", name);

				context.sendBroadcast(pebbleIntent);
				return null;
			});
	}
}
