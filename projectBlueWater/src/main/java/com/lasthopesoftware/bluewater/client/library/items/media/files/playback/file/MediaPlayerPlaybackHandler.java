package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.AbstractPromise;

import java.io.IOException;

/**
 * Created by david on 9/20/16.
 */

public class MediaPlayerPlaybackHandler implements IPlaybackHandler {

	private final MediaPlayer mediaPlayer;

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	@Override
	public void pause() {
		mediaPlayer.pause();
	}

	@Override
	public void seekTo(int pos) {
		mediaPlayer.seekTo(pos);
	}

	@Override
	public int getCurrentPosition() {
		return mediaPlayer.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		return mediaPlayer.getDuration();
	}

	@Override
	public AbstractPromise<IPlaybackHandler> start() {
		return new MediaPlayerPlayerPromise(this, mediaPlayer);
	}

	@Override
	public void close() throws IOException {
		mediaPlayer.release();
	}
}