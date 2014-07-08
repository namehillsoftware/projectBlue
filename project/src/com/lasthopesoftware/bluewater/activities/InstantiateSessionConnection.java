package com.lasthopesoftware.bluewater.activities;

import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class InstantiateSessionConnection extends Activity {
	
	private static final int ACTIVITY_LAUNCH_DELAY = 3000;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connection_status);
		
		final TextView lblConnectionStatus = (TextView)findViewById(R.id.lblConnectionStatus);
		lblConnectionStatus.setText(R.string.lbl_getting_library_details);
		
		final Intent selectServerIntent = new Intent(this, SelectServer.class);
		final InstantiateSessionConnection _this = this;
		
		((Button) findViewById(R.id.btnCancel)).setEnabled(false);
		
		JrSession.GetLibrary(_this, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				final Handler handler = new Handler();
				
				if (result == null || result.getSelectedView() <= 0) {
					lblConnectionStatus.setText(R.string.lbl_please_connect_to_valid_server);
					handler.postDelayed(new LaunchRunnable(_this, selectServerIntent), ACTIVITY_LAUNCH_DELAY);
					return;
				}
				
				lblConnectionStatus.setText(R.string.lbl_connecting_to_server_library);
				final Library library = result;
				
				ConnectionManager.buildConfiguration(_this, library.getAccessCode(), library.getAuthKey(), new OnCompleteListener<Integer, Void, Boolean>() {

					@Override
					public void onComplete(ISimpleTask<Integer, Void, Boolean> owner, Boolean result) {
						if (result == Boolean.FALSE) {
							lblConnectionStatus.setText(R.string.lbl_error_connecting_try_again);
							handler.postDelayed(new LaunchRunnable(_this, selectServerIntent), ACTIVITY_LAUNCH_DELAY);
							return;
						}
						
						
						lblConnectionStatus.setText(R.string.lbl_connected);
						
						JrSession.JrFs = new FileSystem(library.getSelectedView());
						LoggerFactory.getLogger(JrSession.class).debug("Session started.");
						handler.postDelayed(new LaunchRunnable(_this, new Intent(_this, BrowseLibrary.class)), ACTIVITY_LAUNCH_DELAY);
					}
					
				});
			}
			
		});
	}
	
	private static class LaunchRunnable implements Runnable {
		private final Intent mIntent;
		private final Context mContext;
		
		public LaunchRunnable(final Context context, final Intent intent) {
			mIntent = intent;
			mContext = context;
		}
		
		@Override
		public void run() {
			mContext.startActivity(mIntent);
		}
		
	}
}
