package com.lasthopesoftware.bluewater.servers.listeners;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.lasthopesoftware.bluewater.servers.EditServerActivity;

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
		final Intent intent = new Intent(v.getContext(), EditServerActivity.class);
		intent.putExtra(EditServerActivity.serverIdExtra, mServerId);
		mActivity.startActivityForResult(intent, 5388);
	}
}
