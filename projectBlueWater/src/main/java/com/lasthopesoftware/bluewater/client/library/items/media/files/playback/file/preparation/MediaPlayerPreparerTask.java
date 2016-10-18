package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

/**
 * Created by david on 10/3/16.
 */
class MediaPlayerPreparerTask implements TwoParameterRunnable<OneParameterRunnable<IPlaybackHandler>,OneParameterRunnable<Exception>> {

	private final MediaPlayer mediaPlayer;

	MediaPlayerPreparerTask(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void run(OneParameterRunnable<IPlaybackHandler> resolve, OneParameterRunnable<Exception> reject) {
		mediaPlayer.setOnPreparedListener(mp -> resolve.run(new MediaPlayerPlaybackHandler(mp)));

		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			reject.run(new MediaPlayerException(mp, what, extra));
			return true;
		});

		mediaPlayer.prepareAsync();
	}
}
