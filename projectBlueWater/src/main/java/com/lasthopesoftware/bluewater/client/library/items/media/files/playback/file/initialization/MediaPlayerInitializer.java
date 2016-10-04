package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.IoCommon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by david on 9/24/16.
 */
public class MediaPlayerInitializer implements IPlaybackInitialization<MediaPlayer> {
	private Context context;

	public MediaPlayerInitializer(Context context) {
		this.context = context;
	}

	@Override
	public MediaPlayer initializeMediaPlayer(Uri uri) throws IOException {
		final MediaPlayer mediaPlayer = new MediaPlayer(); // initialize it here
//		mediaPlayer.setOnPreparedListener(this);
//		mediaPlayer.setOnErrorListener(this);
//		mediaPlayer.setOnCompletionListener(this);
//		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		setDataSource(mediaPlayer, uri);

		return mediaPlayer;
	}

	private void setDataSource(MediaPlayer mediaPlayer, Uri uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		final Map<String, String> headers = new HashMap<>();
		if (context == null)
			throw new NullPointerException("The file player's context cannot be null");

		if (!uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)) {
			final Library library = LibrarySession.GetActiveLibrary(context);
			if (library != null) {
				final String authKey = library.getAuthKey();

				if (authKey != null && !authKey.isEmpty())
					headers.put("Authorization", "basic " + authKey);
			}
		}

		mediaPlayer.setDataSource(context, uri, headers);
	}
}
