package com.lasthopesoftware.bluewater.client.settings;

import android.app.PendingIntent;

/**
 * Created by david on 7/3/16.
 */
public interface IEditClientSettingsPendingIntentBuilder {
	PendingIntent buildEditServerSettingsPendingIntent(int libraryId);
}
