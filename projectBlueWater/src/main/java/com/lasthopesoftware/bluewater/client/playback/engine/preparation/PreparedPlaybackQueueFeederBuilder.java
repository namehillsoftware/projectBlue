package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.preparation.MediaPlayerPlayableFilePreparationSourceProvider;

public class PreparedPlaybackQueueFeederBuilder implements BuildPreparedPlaybackQueueFeeder {

	private final Context context;
	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;
	private final LookupSelectedPlaybackEngineType playbackEngineTypeLookup;

	public PreparedPlaybackQueueFeederBuilder(
		Context context,
		Handler handler,
		BestMatchUriProvider bestMatchUriProvider,
		LookupSelectedPlaybackEngineType playbackEngineTypeLookup) {

		this.context = context;
		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.playbackEngineTypeLookup = playbackEngineTypeLookup;
	}

	@Override
	public IPlayableFilePreparationSourceProvider build(Library library) {
		switch (playbackEngineTypeLookup.getSelectedPlaybackEngineType()) {
			case ExoPlayer:
				return new ExoPlayerPlayableFilePreparationSourceProvider(
					context,
					handler,
					bestMatchUriProvider,
					library);
			case MediaPlayer:
				return new MediaPlayerPlayableFilePreparationSourceProvider(
					context,
					bestMatchUriProvider,
					library);
		}

		return null;
	}
}
