package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 10/4/16.
 */
class MediaPlayerPlaybackTask implements TwoParameterAction<OneParameterAction<IPlaybackHandler>, OneParameterAction<Exception>> {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;

	MediaPlayerPlaybackTask(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void runWith(OneParameterAction<IPlaybackHandler> resolve, OneParameterAction<Exception> reject) {
		mediaPlayer.setOnCompletionListener(mp -> resolve.runWith(playbackHandler));
		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			final MediaPlayerException mediaPlayerException = new MediaPlayerException(mp, what, extra);
			reject.runWith(mediaPlayerException);
			return true;
		});

		mediaPlayer.start();
	}
}
