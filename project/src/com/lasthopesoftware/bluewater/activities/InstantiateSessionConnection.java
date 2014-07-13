package com.lasthopesoftware.bluewater.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.BuildSessionConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.BuildSessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.BuildSessionConnection.OnBuildSessionStateChangeListener;

public class InstantiateSessionConnection extends Activity {
	
	private static final int ACTIVITY_LAUNCH_DELAY = 3000;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_status);
		
		final TextView lblConnectionStatus = (TextView)findViewById(R.id.lblConnectionStatus);
		
		
		final Intent selectServerIntent = new Intent(this, SelectServer.class);
		final Intent browseLibraryIntent = new Intent(this, BrowseLibrary.class);
		
		BuildSessionConnection.build(this, new OnBuildSessionStateChangeListener() {
			
			@Override
			public void onBuildSessionStatusChange(BuildingSessionConnectionStatus status) {
				switch (status) {
				case GETTING_LIBRARY:
					lblConnectionStatus.setText(R.string.lbl_getting_library_details);
					break;
				case GETTING_LIBRARY_FAILED:
					lblConnectionStatus.setText(R.string.lbl_please_connect_to_valid_server);
					launchActivityDelayed(selectServerIntent);
					break;
				case BUILDING_CONNECTION:
					lblConnectionStatus.setText(R.string.lbl_connecting_to_server_library);
					break;
				case BUILDING_CONNECTION_FAILED:
					lblConnectionStatus.setText(R.string.lbl_error_connecting_try_again);
					launchActivityDelayed(selectServerIntent);
					break;
				case GETTING_VIEW:
					lblConnectionStatus.setText(R.string.lbl_getting_library_views);
					break;
				case GETTING_VIEW_FAILED:
					lblConnectionStatus.setText(R.string.lbl_library_no_views);
					launchActivityDelayed(selectServerIntent);
					break;
				case BUILDING_SESSION_COMPLETE:
					lblConnectionStatus.setText(R.string.lbl_connected);
					launchActivityDelayed(browseLibraryIntent);
					break;
				}
			}
		});
	}
	
	private void launchActivityDelayed(Intent intent) {
		final Handler handler = new Handler();
		handler.postDelayed(new LaunchRunnable(this, intent), ACTIVITY_LAUNCH_DELAY);
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
