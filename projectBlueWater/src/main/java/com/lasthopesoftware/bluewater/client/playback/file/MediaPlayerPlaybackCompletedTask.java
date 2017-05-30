package com.lasthopesoftware.bluewater.client.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.promises.Messenger;
import com.vedsoft.futures.runnables.OneParameterAction;

final class MediaPlayerPlaybackCompletedTask implements OneParameterAction<Messenger<IPlaybackHandler>> {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;

	MediaPlayerPlaybackCompletedTask(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void runWith(Messenger<IPlaybackHandler> messenger) {
		mediaPlayer.setOnCompletionListener(mp -> messenger.sendResolution(playbackHandler));
		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			messenger.sendRejection(new MediaPlayerException(playbackHandler, mp, what, extra));
			return true;
		});
	}
}
