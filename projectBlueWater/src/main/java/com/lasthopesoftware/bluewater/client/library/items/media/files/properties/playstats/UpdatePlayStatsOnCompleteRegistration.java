package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider;

import java.util.Collection;
import java.util.Collections;

public class UpdatePlayStatsOnCompleteRegistration implements IConnectionDependentReceiverRegistration {

	private static final Collection<IntentFilter> intents = Collections.singleton(new IntentFilter(PlaylistEvents.onFileComplete));

	@Override
	public BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider) {
		final FilePropertyCache cache = FilePropertyCache.getInstance();

		return new UpdatePlayStatsOnPlaybackCompleteReceiver(
			new PlaystatsUpdateSelector(
				connectionProvider,
				new FilePropertiesProvider(connectionProvider, cache),
				new FilePropertiesStorage(connectionProvider, cache),
				new ProgramVersionProvider(connectionProvider)));
	}

	@Override
	public Collection<IntentFilter> forIntents() {
		return intents;
	}


}
