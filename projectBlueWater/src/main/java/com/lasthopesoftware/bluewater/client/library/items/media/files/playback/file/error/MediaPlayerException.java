package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;

/**
 * Created by david on 9/21/16.
 */

public class MediaPlayerException extends PlaybackException {
	private final MediaPlayer mediaPlayer;
	public final int what;
	public final int extra;

	public MediaPlayerException(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer, int what, int extra) {
		super(playbackHandler);

		this.mediaPlayer = mediaPlayer;
		this.what = what;
		this.extra = extra;
	}
}
