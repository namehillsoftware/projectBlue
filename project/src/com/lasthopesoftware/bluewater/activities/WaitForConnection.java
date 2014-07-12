package com.lasthopesoftware.bluewater.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnPollingCancelledListener;

public class WaitForConnection extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wait_for_connection);
		
		final Intent selectServerIntent = new Intent(this, SelectServer.class);
		final WaitForConnection _this = this;
		
		PollConnection.Instance.get(_this).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
			
			@Override
			public void onConnectionRegained() {
				finish();
			}
		});
		
		PollConnection.Instance.get(_this).addOnPollingCancelledListener(new OnPollingCancelledListener() {
			
			@Override
			public void onPollingCancelled() {
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
