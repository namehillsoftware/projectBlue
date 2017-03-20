package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

/**
 * Created by david on 3/19/17.
 */

public class UpdatePlayStatsOnCompleteRegistration implements IConnectionDependentReceiverRegistration {

	private final ILibraryProvider libraryProvider;

	public UpdatePlayStatsOnCompleteRegistration(ILibraryProvider libraryProvider) {
		this.libraryProvider = libraryProvider;
	}

	@Override
	public void registerWithConnectionProvider(LocalBroadcastManager localBroadcastManager, IConnectionProvider connectionProvider) {
		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance());
		final UpdatePlayStatsOnPlaybackCompleteReceiver receiver = new UpdatePlayStatsOnPlaybackCompleteReceiver(libraryProvider, filePropertiesProvider);
		localBroadcastManager.registerReceiver(receiver, new IntentFilter(PlaylistEvents.onFileComplete));
	}
}
