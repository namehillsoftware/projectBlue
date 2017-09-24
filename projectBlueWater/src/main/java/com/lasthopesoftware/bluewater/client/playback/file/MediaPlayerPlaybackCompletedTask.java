package com.lasthopesoftware.bluewater.client.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerErrorException;
import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;

final class MediaPlayerPlaybackCompletedTask implements
	MessengerOperator<IPlaybackHandler>,
	MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;
	private Messenger<IPlaybackHandler> playbackHandlerMessenger;

	MediaPlayerPlaybackCompletedTask(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	void play() {
		if (playbackHandlerMessenger == null || isPlaying()) return;

		try {
			mediaPlayer.start();
		} catch (IllegalStateException e) {
			mediaPlayer.release();
			playbackHandlerMessenger.sendRejection(new MediaPlayerException(playbackHandler, mediaPlayer, e));
		}
	}

	boolean isPlaying() {
		try {
			return mediaPlayer.isPlaying();
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public void send(Messenger<IPlaybackHandler> playbackHandlerMessenger) {
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
		playbackHandlerMessenger.sendRejection(new MediaPlayerErrorException(playbackHandler, mp, what, extra));
		return true;
	}
}
