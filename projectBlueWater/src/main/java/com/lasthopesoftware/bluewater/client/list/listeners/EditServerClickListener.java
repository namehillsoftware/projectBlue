package com.lasthopesoftware.bluewater.client.list.listeners;

import android.app.Activity;
import android.view.View;

import com.lasthopesoftware.bluewater.client.settings.EditServerSettingsActivity;

/**
 * Created by david on 7/9/15.
 */
public class EditServerClickListener implements View.OnClickListener {

	private final Activity activity;
	private final int libraryId;

	public EditServerClickListener(Activity activity, int libraryId) {
		this.activity = activity;
		this.libraryId = libraryId;
	}

	@Override
	public void onClick(View v) {
		activity.startActivityForResult(EditServerSettingsActivity.getEditServerSettingsActivityLaunchIntent(v.getContext(), libraryId), 5388);
	}
}
