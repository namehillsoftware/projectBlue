package com.lasthopesoftware.bluewater.client.servers.list.listeners;

import android.app.Activity;
import android.view.View;
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivityIntentBuilder;
import com.lasthopesoftware.bluewater.client.settings.IEditClientSettingsActivityIntentBuilder;

/**
 * Created by david on 7/9/15.
 */
public class EditServerClickListener implements View.OnClickListener {

	private final Activity activity;
	private final IEditClientSettingsActivityIntentBuilder editClientSettingsActivityIntentBuilder;
	private final int libraryId;

	public EditServerClickListener(Activity activity, int libraryId) {
		this(activity, new EditClientSettingsActivityIntentBuilder(activity), libraryId);
	}

	public EditServerClickListener(Activity activity, IEditClientSettingsActivityIntentBuilder editClientSettingsActivityIntentBuilder, int libraryId) {
		this.activity = activity;
		this.editClientSettingsActivityIntentBuilder = editClientSettingsActivityIntentBuilder;
		this.libraryId = libraryId;
	}

	@Override
	public void onClick(View v) {
		activity.startActivityForResult(editClientSettingsActivityIntentBuilder.buildIntent(libraryId), 5388);
	}
}
