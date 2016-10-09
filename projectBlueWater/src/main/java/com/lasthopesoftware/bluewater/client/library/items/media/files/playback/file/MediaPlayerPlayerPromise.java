package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.AbstractPromise;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;

/**
 * Created by david on 10/4/16.
 */
class MediaPlayerPlayerPromise extends AbstractPromise<IPlaybackHandler> {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;

	MediaPlayerPlayerPromise(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	protected void execute() {
		mediaPlayer.setOnCompletionListener(mp -> resolve(playbackHandler));
		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			final MediaPlayerException mediaPlayerException = new MediaPlayerException(mp, what, extra);
			reject(mediaPlayerException);
			return true;
		});

		mediaPlayer.start();
	}
}
