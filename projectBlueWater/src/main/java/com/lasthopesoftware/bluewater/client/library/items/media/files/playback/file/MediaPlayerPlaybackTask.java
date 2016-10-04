package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.vedsoft.fluent.FluentSpecifiedTask;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by david on 10/4/16.
 */
class MediaPlayerPlaybackTask extends FluentSpecifiedTask<Void, Integer, Void> {

	private static final long parkTime = 100000000; // 100 ms

	private final MediaPlayer mediaPlayer;
	private boolean isComplete;
	private MediaPlayerException mediaPlayerException;

	MediaPlayerPlaybackTask(MediaPlayer mediaPlayer, Executor executor) {
		super(executor);
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	protected Void executeInBackground(Void[] params) {
		mediaPlayer.setOnCompletionListener(mp -> isComplete = true);
		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			mediaPlayerException = new MediaPlayerException(mp, what, extra);
			return true;
		});

		mediaPlayer.start();

		while (!isComplete) {
			if (mediaPlayerException != null) {
				setException(mediaPlayerException);
				return null;
			}

			if (isCancelled()) {
				mediaPlayer.stop();
				return null;
			}

			if (!isComplete)
				reportProgress(mediaPlayer.getCurrentPosition());

			LockSupport.parkNanos(parkTime);
		}

		return null;
	}
}
