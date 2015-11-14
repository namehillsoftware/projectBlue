package com.lasthopesoftware.bluewater.servers.list.listeners;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.lasthopesoftware.bluewater.servers.settings.EditServerSettingsActivity;

/**
 * Created by david on 7/9/15.
 */
public class EditServerClickListener implements View.OnClickListener {

	private final Activity mActivity;
	private final int mServerId;

	public EditServerClickListener(Activity activity, int serverId) {
		mActivity = activity;
		mServerId = serverId;
	}

	@Override
	public void onClick(View v) {
		final Intent intent = new Intent(v.getContext(), EditServerSettingsActivity.class);
		intent.putExtra(EditServerSettingsActivity.serverIdExtra, mServerId);
		mActivity.startActivityForResult(intent, 5388);
	}
}
