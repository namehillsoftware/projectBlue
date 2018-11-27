package com.lasthopesoftware.bluewater.client.connection.polling;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

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

		final Promise<IConnectionProvider> pollSessionConnection = PollConnectionService.pollSessionConnection(this);

		findViewById(R.id.btnCancel).setOnClickListener(v -> {
			pollSessionConnection.cancel();
			startActivity(selectServerIntent);
			finish();
		});

		pollSessionConnection
			.then(
				new VoidResponse<>(c -> finish()),
				new VoidResponse<>(e -> {
					if (e instanceof CancellationException) startActivity(selectServerIntent);

					finish();
				}));
	}
}
