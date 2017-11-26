package com.lasthopesoftware.bluewater.client.playback.engine.preferences;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;

public class SelectedPlaybackEngineTypeAccess implements LookupSelectedPlaybackEngineType {

	private final Context context;

	public SelectedPlaybackEngineTypeAccess(Context context) {
		this.context = context;
	}

	@Override
	public PlaybackEngineType getSelectedPlaybackEngineType() {
		return PlaybackEngineType.valueOf(PreferenceManager
			.getDefaultSharedPreferences(context)
			.getString(
				ApplicationConstants.PreferenceConstants.playbackEngine,
				PlaybackEngineType.MediaPlayer.name()));
	}
}
