package com.lasthopesoftware.bluewater.client.playback.engine;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.ExoPlayerPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.mediaplayer.MediaPlayerPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackQueueConfiguration;

public class PlaybackEngineBuilder implements BuildPlaybackEngines {
	private final Context context;
	private final IFileUriProvider fileUriProvider;
	private final Library library;
	private final LookupSelectedPlaybackEngineType playbackEngineTypeLookup;

	public PlaybackEngineBuilder(
		Context context,
		IFileUriProvider fileUriProvider,
		Library library,
		LookupSelectedPlaybackEngineType playbackEngineTypeLookup) {
		this.context = context;
		this.fileUriProvider = fileUriProvider;
		this.library = library;
		this.playbackEngineTypeLookup = playbackEngineTypeLookup;
	}

	@Override
	public <Engine extends IPlaybackPreparerProvider & IPreparedPlaybackQueueConfiguration> Engine build() {
		return playbackEngineTypeLookup.getSelectedPlaybackEngineType() == PlaybackEngineType.MediaPlayer
			? (Engine)new MediaPlayerPlaybackPreparerProvider(
				context,
				fileUriProvider,
				library)
			: (Engine)new ExoPlayerPlaybackPreparerProvider(
				context,
				fileUriProvider,
				library);
	}
}
