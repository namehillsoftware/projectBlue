package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

import java.util.Collection;
import java.util.Collections;

public class UpdatePlayStatsOnCompleteRegistration implements IConnectionDependentReceiverRegistration {

	private static final Collection<IntentFilter> intents = Collections.singleton(new IntentFilter(PlaylistEvents.onFileComplete));

	@Override
	public BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider) {
		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance());
		return new UpdatePlayStatsOnPlaybackCompleteReceiver(connectionProvider, filePropertiesProvider);
	}

	@Override
	public Collection<IntentFilter> forIntents() {
		return intents;
	}


}
