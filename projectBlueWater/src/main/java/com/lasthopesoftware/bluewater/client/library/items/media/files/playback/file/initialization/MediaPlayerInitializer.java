package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;

/**
 * Created by david on 9/24/16.
 */
public class MediaPlayerInitializer implements IPlaybackInitialization<MediaPlayer> {
	private Context context;

	public MediaPlayerInitializer(Context context) {
		this.context = context;
	}

	@Override
	public MediaPlayer initializeMediaPlayer(String uri) {
		final MediaPlayer mediaPlayer = new MediaPlayer(); // initialize it here
//		mediaPlayer.setOnPreparedListener(this);
//		mediaPlayer.setOnErrorListener(this);
//		mediaPlayer.setOnCompletionListener(this);
//		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		return mediaPlayer;
	}
}
