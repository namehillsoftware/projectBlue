package com.lasthopesoftware.bluewater.servers.connection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity;

public class WaitForConnectionActivity extends Activity {
	
	public static void beginWaiting(final Context context, final Runnable onConnectionRegainedListener) {
		final PollConnection pollConnectionInstance = PollConnection.Instance.get(context);
		pollConnectionInstance.addOnConnectionRegainedListener(onConnectionRegainedListener);
		pollConnectionInstance.startPolling();
		context.startActivity(new Intent(context, WaitForConnectionActivity.class));
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wait_for_connection);
		
		final Intent selectServerIntent = new Intent(this, ApplicationSettingsActivity.class);
		
		PollConnection.Instance.get(this).addOnConnectionRegainedListener(this::finish);
		
		PollConnection.Instance.get(this).addOnPollingCancelledListener(() -> startActivity(selectServerIntent));
		
		final Button btnCancel = (Button) findViewById(R.id.btnCancel);
		
		btnCancel.setOnClickListener(v -> {
			PollConnection.Instance.get(v.getContext()).stopPolling();
			startActivity(selectServerIntent);
		});
		
		PollConnection.Instance.get(this).startPolling();
	}
}
