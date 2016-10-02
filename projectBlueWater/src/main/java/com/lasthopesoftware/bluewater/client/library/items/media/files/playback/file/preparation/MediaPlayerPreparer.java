package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.MediaPlayerPlaybackHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by david on 9/24/16.
 */

public class MediaPlayerPreparer implements IPlaybackFilePreparer {

	private static final Logger logger = LoggerFactory.getLogger(MediaPlayerPreparer.class);

	private static final int
			bufferMin = 0,
			bufferMax = 100;

	private final MediaPlayer mediaPlayer;
	private IFile file;

	@SuppressLint("InlinedApi")
	public static final Set<Integer> mediaErrorExtras = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new Integer[]{
			MediaPlayer.MEDIA_ERROR_IO,
			MediaPlayer.MEDIA_ERROR_MALFORMED,
			MediaPlayer.MEDIA_ERROR_UNSUPPORTED,
			MediaPlayer.MEDIA_ERROR_TIMED_OUT,
			MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
	})));

	public MediaPlayerPreparer(MediaPlayer mediaPlayer, IFile file) {
		this.mediaPlayer = mediaPlayer;
		this.file = file;
	}

	@Override
	public IPlaybackHandler getMediaHandler() throws IOException {
		try {
//			final Uri uri = getFileUri();
//			if (uri == null) return;
//
//			setMpDataSource(uri);
//			initializeBufferPercentage(uri);

			logger.info("Preparing " + file.getKey() + " synchronously.");
			mediaPlayer.prepare();

		} catch (IOException io) {
			logger.error(io.toString(), io);
			throw io;
		} catch (Exception e) {
			logger.error(e.toString(), e);
			return null;
		}

		return new MediaPlayerPlaybackHandler(mediaPlayer);
	}
}
