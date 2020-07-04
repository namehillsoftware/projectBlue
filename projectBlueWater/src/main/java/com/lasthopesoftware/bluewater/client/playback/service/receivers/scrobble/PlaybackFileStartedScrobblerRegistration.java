package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.Collection;
import java.util.Collections;

public class PlaybackFileStartedScrobblerRegistration implements IConnectionDependentReceiverRegistration {

	private static final Collection<IntentFilter> intents = Collections.singleton(new IntentFilter(PlaylistEvents.onPlaylistTrackStart));

	@Override
	public BroadcastReceiver registerWithConnectionProvider(IConnectionProvider connectionProvider) {
		final CachedSessionFilePropertiesProvider filePropertiesProvider =
			new CachedSessionFilePropertiesProvider(
				connectionProvider,
				FilePropertyCache.getInstance(),
				new SessionFilePropertiesProvider(
					connectionProvider,
					FilePropertyCache.getInstance()
				));

		return new PlaybackFileChangedScrobbleDroidProxy(filePropertiesProvider, ScrobbleIntentProvider.getInstance());
	}

	@Override
	public Collection<IntentFilter> forIntents() {
		return intents;
	}

	private static class PlaybackFileChangedScrobbleDroidProxy extends BroadcastReceiver {

		private final CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider;
		private final ScrobbleIntentProvider scrobbleIntentProvider;

		public PlaybackFileChangedScrobbleDroidProxy(CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider, ScrobbleIntentProvider scrobbleIntentProvider) {
			this.cachedSessionFilePropertiesProvider = cachedSessionFilePropertiesProvider;
			this.scrobbleIntentProvider = scrobbleIntentProvider;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
			if (fileKey < 0) return;

			cachedSessionFilePropertiesProvider
				.promiseFileProperties(new ServiceFile(fileKey))
				.then(new VoidResponse<>(fileProperties -> {
					final String artist = fileProperties.get(KnownFileProperties.ARTIST);
					final String name = fileProperties.get(KnownFileProperties.NAME);
					final String album = fileProperties.get(KnownFileProperties.ALBUM);
					final long duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);
					final String trackNumberString = fileProperties.get(KnownFileProperties.TRACK);
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
