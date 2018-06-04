package com.lasthopesoftware.bluewater.client.playback.engine.preferences;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;

import java.util.Collections;
import java.util.Set;

public class SelectedPlaybackEngineTypeAccess implements LookupSelectedPlaybackEngineType {

	private static final Set<String> expiredEngineTypes = Collections.singleton("MediaPlayer");

	private final Context context;

	public SelectedPlaybackEngineTypeAccess(Context context) {
		this.context = context;
	}

	@Override
	public PlaybackEngineType getSelectedPlaybackEngineType() {
		final String playbackEngineTypeString = PreferenceManager
			.getDefaultSharedPreferences(context)
			.getString(
				ApplicationConstants.PreferenceConstants.playbackEngine,
				PlaybackEngineType.ExoPlayer.name());

		return expiredEngineTypes.contains(playbackEngineTypeString)
			? PlaybackEngineType.ExoPlayer
			: PlaybackEngineType.valueOf(playbackEngineTypeString);
	}
}
