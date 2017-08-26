package com.lasthopesoftware.bluewater.client.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerTask;

final class MediaPlayerPlaybackCompletedPromise implements
	MessengerTask<IPlaybackHandler>,
	MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;
	private Messenger<IPlaybackHandler> playbackHandlerMessenger;

	MediaPlayerPlaybackCompletedPromise(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void execute(Messenger<IPlaybackHandler> playbackHandlerMessenger) {
		this.playbackHandlerMessenger = playbackHandlerMessenger;

		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		playbackHandlerMessenger.sendResolution(playbackHandler);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		playbackHandlerMessenger.sendRejection(new MediaPlayerException(playbackHandler, mp, what, extra));
		return true;
	}
}
