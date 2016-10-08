package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

/**
 * Created by david on 10/4/16.
 */
class MediaPlayerPlaybackTask implements TwoParameterRunnable<OneParameterRunnable<IPlaybackHandler>, OneParameterRunnable<Exception>> {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;

	MediaPlayerPlaybackTask(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void run(OneParameterRunnable<IPlaybackHandler> resolve, OneParameterRunnable<Exception> reject) {
		mediaPlayer.setOnCompletionListener(mp -> resolve.run(playbackHandler));
		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			final MediaPlayerException mediaPlayerException = new MediaPlayerException(mp, what, extra);
			reject.run(mediaPlayerException);
			return true;
		});

		mediaPlayer.start();
	}
}
