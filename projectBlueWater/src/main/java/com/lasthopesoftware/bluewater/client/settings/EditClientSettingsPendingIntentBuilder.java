package com.lasthopesoftware.bluewater.client.settings;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created by david on 7/3/16.
 */
public class EditClientSettingsPendingIntentBuilder implements IEditClientSettingsPendingIntentBuilder {

	private final Context context;
	private final IEditClientSettingsActivityIntentBuilder editClientSettingsActivityIntentBuilder;

	public EditClientSettingsPendingIntentBuilder(Context context) {
		this(context, new EditClientSettingsActivityIntentBuilder(context));
	}

	public EditClientSettingsPendingIntentBuilder(Context context, IEditClientSettingsActivityIntentBuilder editClientSettingsActivityIntentBuilder) {
		this.context = context;
		this.editClientSettingsActivityIntentBuilder = editClientSettingsActivityIntentBuilder;
	}

	@Override
	public PendingIntent buildEditServerSettingsPendingIntent(int libraryId) {
		final Intent settingsIntent = editClientSettingsActivityIntentBuilder.buildIntent(libraryId);
		settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		return PendingIntent.getActivity(context, 0, settingsIntent, 0);
	}
}
