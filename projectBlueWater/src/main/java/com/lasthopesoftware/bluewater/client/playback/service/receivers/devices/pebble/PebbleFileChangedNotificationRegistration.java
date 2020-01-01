package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.pebble;


import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;

import java.util.Collection;
import java.util.Collections;

public class PebbleFileChangedNotificationRegistration implements IConnectionDependentReceiverRegistration {

	private static final Collection<IntentFilter> intents = Collections.singleton(new IntentFilter(PlaylistEvents.onPlaylistChange));

	@Override
	public BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider) {
		final CachedSessionFilePropertiesProvider filePropertiesProvider =
			new CachedSessionFilePropertiesProvider(
				connectionProvider,
				FilePropertyCache.getInstance(),
				new SessionFilePropertiesProvider(
					connectionProvider,
					FilePropertyCache.getInstance(),
					ParsingScheduler.instance()));

		return new PebbleFileChangedProxy(filePropertiesProvider);
	}

	@Override
	public Collection<IntentFilter> forIntents() {
		return intents;
	}
}
