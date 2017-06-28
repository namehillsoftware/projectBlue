package com.lasthopesoftware.bluewater.client.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.promises.Promise;

final class MediaPlayerPlaybackCompletedPromise extends Promise<IPlaybackHandler>
	implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;

	MediaPlayerPlaybackCompletedPromise(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;

		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
	}


	@Override
	public void onCompletion(MediaPlayer mp) {
		sendResolution(playbackHandler);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		sendRejection(new MediaPlayerException(playbackHandler, mp, what, extra));
		return true;
	}
}
