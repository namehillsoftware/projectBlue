package com.lasthopesoftware.bluewater.client.playback.file.error;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;

public class MediaPlayerException extends PlaybackException {
	public final MediaPlayer mediaPlayer;

	public MediaPlayerException(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer, Throwable cause) {
		super(playbackHandler, cause);
		this.mediaPlayer = mediaPlayer;
	}
}
