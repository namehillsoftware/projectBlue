package com.lasthopesoftware.bluewater.client.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerErrorException;
import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;
import java.util.concurrent.CancellationException;

public final class MediaPlayerPlaybackHandler
implements
	IPlaybackHandler,
	MessengerOperator<IPlaybackHandler>,
	MediaPlayer.OnCompletionListener,
	MediaPlayer.OnErrorListener,
	Runnable {

	private final MediaPlayer mediaPlayer;
	private float volume;
	private final Promise<IPlaybackHandler> playbackPromise;

	private Messenger<IPlaybackHandler> playbackHandlerMessenger;

	private int previousMediaPlayerPosition;

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		playbackPromise = new Promise<>((MessengerOperator<IPlaybackHandler>) this);
	}

	@Override
	public boolean isPlaying() {
		try {
			return mediaPlayer.isPlaying();
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public void pause() {
		mediaPlayer.pause();
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;
		mediaPlayer.setVolume(volume, volume);
	}

	@Override
	public float getVolume() {
		return volume;
	}

	@Override
	public int getCurrentPosition() {
		return isPlaying()
			? previousMediaPlayerPosition = mediaPlayer.getCurrentPosition()
			: previousMediaPlayerPosition;
	}

	@Override
	public int getDuration() {
		try {
			return mediaPlayer.getDuration();
		} catch (IllegalStateException e) {
			return 0;
		}
	}

	@Override
	public synchronized Promise<IPlaybackHandler> promisePlayback() {
		if (isPlaying()) return playbackPromise;

		try {
			mediaPlayer.start();
		} catch (IllegalStateException e) {
			mediaPlayer.release();
			playbackHandlerMessenger.sendRejection(new MediaPlayerException(this, mediaPlayer, e));
		}

		return playbackPromise;
	}

	@Override
	public void send(Messenger<IPlaybackHandler> playbackHandlerMessenger) {
		this.playbackHandlerMessenger = playbackHandlerMessenger;

		playbackHandlerMessenger.cancellationRequested(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		playbackHandlerMessenger.sendResolution(this);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		playbackHandlerMessenger.sendRejection(new MediaPlayerErrorException(this, mp, what, extra));
		return true;
	}

	@Override
	public void close() throws IOException {
		mediaPlayer.release();
	}

	@Override
	public void run() {
		try {
			close();
			playbackHandlerMessenger.sendRejection(new CancellationException());
		} catch (IOException e) {
			playbackHandlerMessenger.sendRejection(e);
		}
	}
}