package com.lasthopesoftware.bluewater.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;

/**
 * Created by david on 11/14/15.
 */
public class HandleCheckboxPreference extends AsyncTask<Void, Void, Boolean> {
	private final String settingKey;
	private final CheckBox settingCheckbox;
	private final SharedPreferences sharedPreferences;

	public static void handle(Context context, String settingKey, CheckBox settingCheckbox) {
		new HandleCheckboxPreference(context, settingKey, settingCheckbox).execute();
	}

	private HandleCheckboxPreference(Context context, String settingKey, CheckBox settingCheckbox) {
		settingCheckbox.setEnabled(false);
		this.settingKey = settingKey;
		this.settingCheckbox = settingCheckbox;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		return sharedPreferences.getBoolean(settingKey, false);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);

		settingCheckbox.setChecked(result);
		settingCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				sharedPreferences
						.edit()
						.putBoolean(settingKey, isChecked)
						.apply();
			}
		});
		settingCheckbox.setEnabled(true);
	}
}
