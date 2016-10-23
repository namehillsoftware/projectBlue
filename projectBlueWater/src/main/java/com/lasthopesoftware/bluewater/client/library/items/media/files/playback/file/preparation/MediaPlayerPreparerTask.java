package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 10/3/16.
 */
class MediaPlayerPreparerTask implements TwoParameterAction<IResolvedPromise<IPlaybackHandler>, IRejectedPromise> {

	private final MediaPlayer mediaPlayer;

	MediaPlayerPreparerTask(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void runWith(IResolvedPromise<IPlaybackHandler> resolve, IRejectedPromise reject) {
		mediaPlayer.setOnPreparedListener(mp -> resolve.withResult(new MediaPlayerPlaybackHandler(mp)));

		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			reject.withError(new MediaPlayerException(mp, what, extra));
			return true;
		});

		mediaPlayer.prepareAsync();
	}
}
