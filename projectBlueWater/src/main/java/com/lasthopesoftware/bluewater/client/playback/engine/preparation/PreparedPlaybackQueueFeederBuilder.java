package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.preparation.MediaPlayerPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.compilation.FlagCompilationForDebugging;

public class PreparedPlaybackQueueFeederBuilder implements BuildPreparedPlaybackQueueFeeder {

	private final Context context;
	private final BestMatchUriProvider bestMatchUriProvider;
	private final RemoteFileUriProvider remoteFileUriProvider;
	private final LookupSelectedPlaybackEngineType playbackEngineTypeLookup;
	private final FlagCompilationForDebugging flagCompilationForDebugging;

	public PreparedPlaybackQueueFeederBuilder(
		Context context,
		BestMatchUriProvider bestMatchUriProvider,
		RemoteFileUriProvider remoteFileUriProvider,
		LookupSelectedPlaybackEngineType playbackEngineTypeLookup,
		FlagCompilationForDebugging flagCompilationForDebugging) {

		this.context = context;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.remoteFileUriProvider = remoteFileUriProvider;
		this.playbackEngineTypeLookup = playbackEngineTypeLookup;
		this.flagCompilationForDebugging = flagCompilationForDebugging;
	}

	@Override
	public IPlayableFilePreparationSourceProvider build(Library library) {
		switch (playbackEngineTypeLookup.getSelectedPlaybackEngineType()) {
			case ExoPlayer:
				if (flagCompilationForDebugging.isDebugCompilation())
					return new ExoPlayerPlayableFilePreparationSourceProvider(
						context,
						bestMatchUriProvider,
						remoteFileUriProvider,
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
