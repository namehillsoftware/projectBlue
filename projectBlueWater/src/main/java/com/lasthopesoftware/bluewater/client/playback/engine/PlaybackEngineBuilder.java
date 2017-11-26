package com.lasthopesoftware.bluewater.client.playback.engine;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.ExoPlayerPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.mediaplayer.MediaPlayerPlaybackPreparerProvider;
import com.lasthopesoftware.compilation.FlagCompilationForDebugging;

public class PlaybackEngineBuilder implements BuildPlaybackEngines {

	private final Context context;
	private final IFileUriProvider fileUriProvider;
	private final LookupSelectedPlaybackEngineType playbackEngineTypeLookup;
	private final FlagCompilationForDebugging flagCompilationForDebugging;

	public PlaybackEngineBuilder(
		Context context,
		IFileUriProvider fileUriProvider,
		LookupSelectedPlaybackEngineType playbackEngineTypeLookup,
		FlagCompilationForDebugging flagCompilationForDebugging) {

		this.context = context;
		this.fileUriProvider = fileUriProvider;
		this.playbackEngineTypeLookup = playbackEngineTypeLookup;
		this.flagCompilationForDebugging = flagCompilationForDebugging;
	}

	@Override
	public PlaybackEngine build(Library library) {
		switch (playbackEngineTypeLookup.getSelectedPlaybackEngineType()) {
			case ExoPlayer:
				if (flagCompilationForDebugging.isDebugCompilation())
					return new ExoPlayerPlaybackPreparerProvider(
						context,
						fileUriProvider,
						library);
			case MediaPlayer:
				return new MediaPlayerPlaybackPreparerProvider(
					context,
					fileUriProvider,
					library);
		}

		return null;
	}
}
