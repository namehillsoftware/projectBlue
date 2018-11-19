package com.lasthopesoftware.bluewater.client.connection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService;
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.concurrent.CancellationException;

public class WaitForConnectionActivity extends Activity {

	public static void beginWaiting(final Context context, final Runnable onConnectionRegainedListener) {
		context.startActivity(new Intent(context, WaitForConnectionActivity.class));
		PollConnectionService.pollSessionConnection(context)
			.then(c -> {
				onConnectionRegainedListener.run();
				return null;
			});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wait_for_connection);
		
		final Intent selectServerIntent = new Intent(this, ApplicationSettingsActivity.class);

		Promise<IConnectionProvider> pollSessionConnection = PollConnectionService.pollSessionConnection(this);
		final Button btnCancel = findViewById(R.id.btnCancel);

		btnCancel.setOnClickListener(v -> {
			pollSessionConnection.cancel();
			startActivity(selectServerIntent);
		});

		pollSessionConnection
			.then(
				c -> {
					finish();
					return null;
				},
				e -> {
					if (e instanceof CancellationException) {
						startActivity(selectServerIntent);
					}

					return null;
				});
	}
}
