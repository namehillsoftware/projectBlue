package com.lasthopesoftware.bluewater.client.connection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService;
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity;

public class WaitForConnectionActivity extends Activity {
	
	public static void beginWaiting(final Context context, final Runnable onConnectionRegainedListener) {
		PollConnectionService.Instance.promise(context)
			.then(pollConnectionInstance -> {
				pollConnectionInstance.addOnConnectionRegainedListener(onConnectionRegainedListener);
				pollConnectionInstance.startPolling();
				context.startActivity(new Intent(context, WaitForConnectionActivity.class));

				return null;
			});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wait_for_connection);
		
		final Intent selectServerIntent = new Intent(this, ApplicationSettingsActivity.class);

		PollConnectionService.Instance.promise(this)
			.then(pollConnectionService -> {
				pollConnectionService.addOnConnectionRegainedListener(this::finish);

				pollConnectionService.addOnPollingCancelledListener(() -> startActivity(selectServerIntent));

				final Button btnCancel = findViewById(R.id.btnCancel);

				btnCancel.setOnClickListener(v -> {
					pollConnectionService.stopPolling();
					startActivity(selectServerIntent);
				});

				pollConnectionService.startPolling();

				return null;
			});
	}
}
