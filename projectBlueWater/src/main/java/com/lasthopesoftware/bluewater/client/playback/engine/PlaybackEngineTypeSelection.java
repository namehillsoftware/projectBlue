package com.lasthopesoftware.bluewater.client.playback.engine;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;

public class PlaybackEngineTypeSelection implements SelectPlaybackEngineType {

	private final Context context;

	public PlaybackEngineTypeSelection(Context context) {
		this.context = context;
	}

	@Override
	public void selectPlaybackEngine(PlaybackEngineType playbackEngineType) {
		PreferenceManager
			.getDefaultSharedPreferences(context).edit()
			.putString(ApplicationConstants.PreferenceConstants.playbackEngine, playbackEngineType.name())
			.apply();
	}
}
