package com.lasthopesoftware.bluewater.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class WaitForConnection extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wait_for_connection);
		
		final Intent selectServerIntent = new Intent(this, SelectServer.class);
		final WaitForConnection _this = this;
		
		PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
				if (result) finish();
				else _this.startActivity(selectServerIntent);
//				if (!result) _this.startActivity(selectServerIntent);
			}
		});
		
		Button btnCancel = (Button) findViewById(R.id.btnCancel);
		
		btnCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				PollConnectionTask.Instance.get().stopPolling();
				_this.startActivity(selectServerIntent);
			}
		});
		
		PollConnectionTask.Instance.get().startPolling();
	}
}
