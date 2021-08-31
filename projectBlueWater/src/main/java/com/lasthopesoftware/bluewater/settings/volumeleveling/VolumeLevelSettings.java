package com.lasthopesoftware.bluewater.settings.volumeleveling;


import android.content.Context;
import android.preference.PreferenceManager;

public class VolumeLevelSettings implements IVolumeLevelSettings {

	private final Context context;

	public VolumeLevelSettings(Context context) {
		this.context = context;
	}

	@Override
	public boolean isVolumeLevellingEnabled() {
		return
			PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(ApplicationConstants.PreferenceConstants.isVolumeLevelingEnabled, false);
	}
}
