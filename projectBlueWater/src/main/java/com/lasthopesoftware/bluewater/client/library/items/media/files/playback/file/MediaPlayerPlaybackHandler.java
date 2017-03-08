package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.MediaPlayerBufferedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 9/20/16.
 */

public final class MediaPlayerPlaybackHandler implements IBufferingPlaybackHandler {

	private final MediaPlayer mediaPlayer;
	private final IPromise<IBufferingPlaybackHandler> bufferingPromise;
	private float volume;
	private final IPromise<IPlaybackHandler> playbackPromise;

	private int previousMediaPlayerPosition;

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		playbackPromise = new Promise<>(new MediaPlayerPlaybackCompletedTask(this, mediaPlayer));
		bufferingPromise = new Promise<>(new MediaPlayerBufferedPromise(this, mediaPlayer));
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
	public synchronized IPromise<IPlaybackHandler> promisePlayback() {
		if (!isPlaying()) {
			try {
				mediaPlayer.start();
			} catch (IllegalStateException e) {
				mediaPlayer.release();
			}
		}

		return playbackPromise;
	}

	@Override
	public void close() throws IOException {
		playbackPromise.cancel();
		mediaPlayer.release();
	}

	@Override
	public IPromise<IBufferingPlaybackHandler> bufferPlaybackFile() {
		return bufferingPromise;
	}
}