package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.MediaPlayerPlaybackHandler;
import com.vedsoft.fluent.FluentCallable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by david on 10/3/16.
 */
class MediaPlayerPreparerTask extends FluentCallable<IPlaybackHandler> {
	private static final Logger logger = LoggerFactory.getLogger(MediaPlayerPreparerTask.class);

	private final MediaPlayer mediaPlayer;

	MediaPlayerPreparerTask(MediaPlayer mediaPlayer) {
		super(AsyncTask.THREAD_POOL_EXECUTOR);

		this.mediaPlayer = mediaPlayer;
	}

	@Override
	protected IPlaybackHandler executeInBackground() {
		try {
			mediaPlayer.prepare();

		} catch (IOException io) {
			logger.error(io.toString(), io);
			setException(io);
		} catch (Exception e) {
			logger.error(e.toString(), e);
			return null;
		}

		return new MediaPlayerPlaybackHandler(mediaPlayer);
	}
}
