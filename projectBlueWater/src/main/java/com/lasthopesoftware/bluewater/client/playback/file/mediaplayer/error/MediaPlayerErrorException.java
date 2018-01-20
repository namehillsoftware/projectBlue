package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MediaPlayerErrorException extends PlaybackException {
	private static final CreateAndHold<Set<Integer>> mediaErrorExtrasLazy = new AbstractSynchronousLazy<Set<Integer>>() {
		@Override
		protected Set<Integer> create() throws Exception {
			return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MediaPlayer.MEDIA_ERROR_IO,
				MediaPlayer.MEDIA_ERROR_MALFORMED,
				MediaPlayer.MEDIA_ERROR_UNSUPPORTED,
				MediaPlayer.MEDIA_ERROR_TIMED_OUT,
				MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK)));
		}
	};

	public final MediaPlayer mediaPlayer;
	public final int what;
	public final int extra;

	public MediaPlayerErrorException(PlayableFile playbackHandler, MediaPlayer mediaPlayer, int what, int extra) {
		super(playbackHandler);

		this.mediaPlayer = mediaPlayer;
		this.what = what;
		this.extra = extra;
	}

	public static Set<Integer> mediaErrorExtras() {
		return mediaErrorExtrasLazy.getObject();
	}
}
