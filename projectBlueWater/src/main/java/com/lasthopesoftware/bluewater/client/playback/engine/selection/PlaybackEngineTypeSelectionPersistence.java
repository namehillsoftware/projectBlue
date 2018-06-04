package com.lasthopesoftware.bluewater.client.playback.engine.selection;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;

public class PlaybackEngineTypeSelectionPersistence implements SelectPlaybackEngineType {

	private final Context context;
	private final PlaybackEngineTypeChangedBroadcaster playbackEngineTypeChangedBroadcaster;

	public PlaybackEngineTypeSelectionPersistence(Context context, PlaybackEngineTypeChangedBroadcaster playbackEngineTypeChangedBroadcaster) {
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
