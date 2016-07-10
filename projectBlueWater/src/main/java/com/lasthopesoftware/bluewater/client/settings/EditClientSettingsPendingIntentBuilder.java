package com.lasthopesoftware.bluewater.client.settings;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created by david on 7/3/16.
 */
public class EditClientSettingsPendingIntentBuilder implements IEditClientSettingsPendingIntentBuilder {

	private final Context context;

	public EditClientSettingsPendingIntentBuilder(Context context) {
		this.context = context;
	}

	@Override
	public PendingIntent buildEditServerSettingsPendingIntent(int libraryId) {
		final Intent settingsIntent = EditClientSettingsActivity.getEditServerSettingsActivityLaunchIntent(context, libraryId);
		settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		settingsIntent.putExtra(EditClientSettingsActivity.serverIdExtra, libraryId);

		return PendingIntent.getActivity(context, 0, settingsIntent, 0);
	}
}
