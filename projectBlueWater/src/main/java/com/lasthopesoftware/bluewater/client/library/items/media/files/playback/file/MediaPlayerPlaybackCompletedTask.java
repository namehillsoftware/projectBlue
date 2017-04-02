package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.promises.EmptyMessenger;

/**
 * Created by david on 10/4/16.
 */
final class MediaPlayerPlaybackCompletedTask extends EmptyMessenger<IPlaybackHandler> {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;

	MediaPlayerPlaybackCompletedTask(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	protected void requestResolution() {
		mediaPlayer.setOnCompletionListener(mp -> withResult(playbackHandler));
		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			withError(new MediaPlayerException(playbackHandler, mp, what, extra));
			return true;
		});
	}
}
