package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.IPlaybackFileErrorBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 9/20/16.
 */

public class MediaPlayerPlaybackHandler implements IPlaybackHandler, MediaPlayer.OnErrorListener {

	private static final ExecutorService playbackExecutor = Executors.newSingleThreadExecutor();

	private final MediaPlayer mediaPlayer;
	private TwoParameterRunnable<IPlaybackFileErrorBroadcaster, MediaPlayerException> onFileErrorListener;
	private IFluentTask<Void, Integer, Void> mediaPlayerTask;

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
	public IFluentTask<Void, Integer, Void> start() {
		mediaPlayerTask = new MediaPlayerPlaybackTask(mediaPlayer, playbackExecutor).execute();
		return mediaPlayerTask;
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		broadcastFileError(new MediaPlayerException(mediaPlayer, what, extra));
		return true;
	}

	@Override
	public void setOnFileErrorListener(TwoParameterRunnable<IPlaybackFileErrorBroadcaster, MediaPlayerException> listener) {
		this.onFileErrorListener = listener;
	}

	@Override
	public void broadcastFileError(MediaPlayerException mediaPlayerException) {
		if (this.onFileErrorListener != null)
			this.onFileErrorListener.run(this, mediaPlayerException);
	}

	@Override
	public void close() throws IOException {
		mediaPlayer.release();

		if (mediaPlayerTask != null)
			mediaPlayerTask.cancel();
	}

}