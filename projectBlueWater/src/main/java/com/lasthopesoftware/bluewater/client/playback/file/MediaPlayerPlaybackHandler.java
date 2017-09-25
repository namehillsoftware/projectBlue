package com.lasthopesoftware.bluewater.client.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.BufferingMediaPlayerTask;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 9/20/16.
 */

public final class MediaPlayerPlaybackHandler
implements IBufferingPlaybackFile, IPlaybackHandler {

	private final MediaPlayer mediaPlayer;
	private final Promise<IBufferingPlaybackFile> bufferingPromise;
	private final MediaPlayerPlaybackCompletedTask mediaPlayerTask;
	private float volume;
	private final Promise<IPlaybackHandler> playbackPromise;

	private int previousMediaPlayerPosition;

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		mediaPlayerTask = new MediaPlayerPlaybackCompletedTask(this, mediaPlayer);
		playbackPromise = new Promise<>(mediaPlayerTask);
		bufferingPromise = new Promise<>(new BufferingMediaPlayerTask(this, mediaPlayer));
	}

	@Override
	public boolean isPlaying() {
		return mediaPlayerTask.isPlaying();
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
		mediaPlayerTask.play();

		return playbackPromise;
	}

	@Override
	public void close() throws IOException {
		playbackPromise.cancel();
		mediaPlayer.release();
	}

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return bufferingPromise;
	}
}