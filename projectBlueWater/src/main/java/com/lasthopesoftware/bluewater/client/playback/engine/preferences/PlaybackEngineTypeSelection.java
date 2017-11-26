package com.lasthopesoftware.bluewater.client.playback.engine.preferences;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.broadcast.PlaybackEngineTypeChangedBroadcaster;

public class PlaybackEngineTypeSelection implements SelectPlaybackEngineType {

	private final Context context;
	private final PlaybackEngineTypeChangedBroadcaster playbackEngineTypeChangedBroadcaster;

	public PlaybackEngineTypeSelection(Context context, PlaybackEngineTypeChangedBroadcaster playbackEngineTypeChangedBroadcaster) {
		this.context = context;
		this.playbackEngineTypeChangedBroadcaster = playbackEngineTypeChangedBroadcaster;
	}

	@Override
	public void selectPlaybackEngine(PlaybackEngineType playbackEngineType) {
		PreferenceManager
			.getDefaultSharedPreferences(context).edit()
			.putString(ApplicationConstants.PreferenceConstants.playbackEngine, playbackEngineType.name())
			.apply();

		playbackEngineTypeChangedBroadcaster.broadcastPlaybackEngineTypeChanged(playbackEngineType);
	}
}
