package com.lasthopesoftware.bluewater.client.playback.engine.selection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.LookupDefaultPlaybackEngine;
import com.namehillsoftware.handoff.promises.Promise;

public class SelectedPlaybackEngineTypeAccess implements LookupSelectedPlaybackEngineType {

	private final Context context;
	private final LookupDefaultPlaybackEngine defaultPlaybackEngineLookup;

	public SelectedPlaybackEngineTypeAccess(Context context, LookupDefaultPlaybackEngine defaultPlaybackEngineLookup) {
		this.context = context;
		this.defaultPlaybackEngineLookup = defaultPlaybackEngineLookup;
	}

	@Override
	public Promise<PlaybackEngineType> promiseSelectedPlaybackEngineType() {
		final SharedPreferences preferences = PreferenceManager
			.getDefaultSharedPreferences(context);

		final String playbackEngineTypeString = preferences
			.getString(
				ApplicationConstants.PreferenceConstants.playbackEngine,
				PlaybackEngineType.ExoPlayer.name());

		if (Stream.of(PlaybackEngineType.values()).map(Enum::name).anyMatch(playbackEngineTypeString::equals))
			return new Promise<>(PlaybackEngineType.valueOf(playbackEngineTypeString));

		return defaultPlaybackEngineLookup.promiseDefaultEngineType()
			.then(t -> {
				preferences
					.edit()
					.putString(
						ApplicationConstants.PreferenceConstants.playbackEngine,
						t.name())
					.apply();
				return null;
			})
			.then(n -> PlaybackEngineType.valueOf(preferences
				.getString(
					ApplicationConstants.PreferenceConstants.playbackEngine,
					PlaybackEngineType.ExoPlayer.name())));

	}
}
