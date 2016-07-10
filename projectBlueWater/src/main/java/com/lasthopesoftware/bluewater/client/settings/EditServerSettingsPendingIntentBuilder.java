package com.lasthopesoftware.bluewater.client.settings;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created by david on 7/3/16.
 */
public class EditServerSettingsPendingIntentBuilder implements IEditServerSettingsPendingIntentBuilder {

	private final Context context;

	public EditServerSettingsPendingIntentBuilder(Context context) {
		this.context = context;
	}

	@Override
	public PendingIntent buildEditServerSettingsPendingIntent(int libraryId) {
		final Intent settingsIntent = EditServerSettingsActivity.getEditServerSettingsActivityLaunchIntent(context, libraryId);
		settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		settingsIntent.putExtra(EditServerSettingsActivity.serverIdExtra, libraryId);

		return PendingIntent.getActivity(context, 0, settingsIntent, 0);
	}
}
