package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.vedsoft.fluent.FluentCallable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by david on 9/24/16.
 */

public class MediaPlayerPreparer implements IPlaybackFilePreparer {

	private final MediaPlayer mediaPlayer;

	public MediaPlayerPreparer(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public FluentCallable<IPlaybackHandler> getMediaHandler() {
		final FluentCallable<IPlaybackHandler> mediaHandlerTask =
				new MediaPlayerPreparerTask(mediaPlayer);

		mediaHandlerTask.execute();

		return mediaHandlerTask;
	}
}
