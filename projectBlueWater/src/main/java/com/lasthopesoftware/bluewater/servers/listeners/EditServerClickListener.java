package com.lasthopesoftware.bluewater.servers.listeners;

import android.content.Intent;
import android.view.View;

import com.lasthopesoftware.bluewater.servers.EditServerActivity;

/**
 * Created by david on 7/9/15.
 */
public class EditServerClickListener implements View.OnClickListener {

	@Override
	public void onClick(View v) {
		v.getContext().startActivity(new Intent(v.getContext(), EditServerActivity.class));
	}
}
