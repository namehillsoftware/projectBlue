package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.IPlaybackFileErrorBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerErrorData;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.io.IOException;

/**
 * Created by david on 9/20/16.
 */

public class MediaPlayerPlaybackHandler implements IPlaybackHandler, MediaPlayer.OnErrorListener {
	private final MediaPlayer mediaPlayer;
	private TwoParameterRunnable<IPlaybackFileErrorBroadcaster, MediaPlayerErrorData> onFileErrorListener;

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		this.mediaPlayer.setOnErrorListener(this);
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
	public void start() {
		mediaPlayer.start();
	}

	@Override
	public void stop() {
		mediaPlayer.stop();
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		broadcastFileError(new MediaPlayerErrorData(mediaPlayer, what, extra));
		return true;
	}

	@Override
	public void setOnFileErrorListener(TwoParameterRunnable<IPlaybackFileErrorBroadcaster, MediaPlayerErrorData> listener) {
		this.onFileErrorListener = listener;
	}

	@Override
	public void broadcastFileError(MediaPlayerErrorData mediaPlayerErrorData) {
		if (this.onFileErrorListener != null)
			this.onFileErrorListener.run(this, mediaPlayerErrorData);
	}

	@Override
	public void close() throws IOException {
		mediaPlayer.release();
	}
}