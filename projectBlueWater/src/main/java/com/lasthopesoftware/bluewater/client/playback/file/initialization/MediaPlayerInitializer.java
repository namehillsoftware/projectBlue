package com.lasthopesoftware.bluewater.client.playback.file.initialization;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by david on 9/24/16.
 */
public final class MediaPlayerInitializer implements IPlaybackInitialization<MediaPlayer> {
	private Context context;
	private final Library library;

	public MediaPlayerInitializer(@NonNull Context context, @NonNull Library library) {
		this.context = context;
		this.library = library;
	}

	@Override
	public MediaPlayer initializeMediaPlayer(@NonNull Uri uri) throws IOException {
		final MediaPlayer mediaPlayer = new MediaPlayer(); // initialize it here
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		final Map<String, String> headers = new HashMap<>();
		if (context == null)
			throw new NullPointerException("The serviceFile player's context cannot be null");

		if (!uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)) {
			final String authKey = library.getAuthKey();

			if (authKey != null && !authKey.isEmpty())
				headers.put("Authorization", "basic " + authKey);
		}

		mediaPlayer.setDataSource(context, uri, headers);

		return mediaPlayer;
	}
}
