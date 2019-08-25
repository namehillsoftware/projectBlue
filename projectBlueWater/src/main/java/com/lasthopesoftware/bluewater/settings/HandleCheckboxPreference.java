package com.lasthopesoftware.bluewater.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import com.lasthopesoftware.bluewater.client.stored.scheduling.SyncSchedulingWorker;

/**
 * Created by david on 11/14/15.
 */
class HandleCheckboxPreference {
	static void handle(Context context, String settingKey, CheckBox settingCheckbox) {
		settingCheckbox.setEnabled(false);
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		final boolean preference = sharedPreferences.getBoolean(settingKey, false);
		settingCheckbox.setChecked(preference);
		settingCheckbox.setOnCheckedChangeListener(
			(buttonView, isChecked) ->
			{
				sharedPreferences
					.edit()
					.putBoolean(settingKey, isChecked)
					.apply();

				SyncSchedulingWorker.scheduleSync(buttonView.getContext());
			});
		settingCheckbox.setEnabled(true);
	}
}
