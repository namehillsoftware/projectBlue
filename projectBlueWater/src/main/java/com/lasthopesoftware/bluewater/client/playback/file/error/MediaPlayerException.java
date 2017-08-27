package com.lasthopesoftware.bluewater.client.playback.file.error;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by david on 9/21/16.
 */

public class MediaPlayerException extends PlaybackException {
	private static final ILazy<Set<Integer>> mediaErrorExtrasLazy = new AbstractSynchronousLazy<Set<Integer>>() {
		@Override
		protected Set<Integer> initialize() throws Exception {
			return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new Integer[]{
				MediaPlayer.MEDIA_ERROR_IO,
				MediaPlayer.MEDIA_ERROR_MALFORMED,
				MediaPlayer.MEDIA_ERROR_UNSUPPORTED,
				MediaPlayer.MEDIA_ERROR_TIMED_OUT,
				MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
			})));
		}
	};

	public final MediaPlayer mediaPlayer;
	public final int what;
	public final int extra;

	public MediaPlayerException(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer, int what, int extra) {
		super(playbackHandler);

		this.mediaPlayer = mediaPlayer;
		this.what = what;
		this.extra = extra;
	}

	public static Set<Integer> mediaErrorExtras() {
		return mediaErrorExtrasLazy.getObject();
	}
}
