package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.pebble;


import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

import java.util.Collection;
import java.util.Collections;

public class PebbleFileChangedNotificationRegistration implements IConnectionDependentReceiverRegistration {

	private static final Collection<IntentFilter> intents = Collections.singleton(new IntentFilter(PlaylistEvents.onPlaylistTrackChange));

	@Override
	public BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider) {
		final ScopedCachedFilePropertiesProvider filePropertiesProvider =
			new ScopedCachedFilePropertiesProvider(
				connectionProvider,
				FilePropertyCache.getInstance(),
				new ScopedFilePropertiesProvider(
					connectionProvider,
					FilePropertyCache.getInstance()
				));

		return new PebbleFileChangedProxy(filePropertiesProvider);
	}

	@Override
	public Collection<IntentFilter> forIntents() {
		return intents;
	}
}
