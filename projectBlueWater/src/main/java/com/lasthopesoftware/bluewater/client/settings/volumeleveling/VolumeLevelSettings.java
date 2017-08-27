package com.lasthopesoftware.bluewater.client.settings.volumeleveling;


import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;

public class VolumeLevelSettings implements IVolumeLevelSettings {

	private Context context;

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
