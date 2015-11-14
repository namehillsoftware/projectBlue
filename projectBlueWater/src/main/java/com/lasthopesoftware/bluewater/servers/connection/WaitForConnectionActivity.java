package com.lasthopesoftware.bluewater.servers.connection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.lasthopesoftware.bluewater.AppSettingsActivity;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;

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
		
		final Intent selectServerIntent = new Intent(this, AppSettingsActivity.class);
		final WaitForConnectionActivity _this = this;
		
		PollConnection.Instance.get(_this).addOnConnectionRegainedListener(new Runnable() {
			
			@Override
			public void run() {
				finish();
			}
		});
		
		PollConnection.Instance.get(_this).addOnPollingCancelledListener(new Runnable() {
			
			@Override
			public void run() {
				_this.startActivity(selectServerIntent);
			}
		});
		
		Button btnCancel = (Button) findViewById(R.id.btnCancel);
		
		btnCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				PollConnection.Instance.get(_this).stopPolling();
				_this.startActivity(selectServerIntent);
			}
		});
		
		PollConnection.Instance.get(_this).startPolling();
	}
}
