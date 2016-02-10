package com.lasthopesoftware.bluewater.servers.connection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.servers.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity;

public class InstantiateSessionConnectionActivity extends Activity {
	
	public static final int ACTIVITY_ID = 2032;
	
	private static final String START_ACTIVITY_FOR_RETURN = "com.lasthopesoftware.bluewater.activities.InstantiateSessionConnection.START_ACTIVITY_FOR_RETURN";
	
	private static final int ACTIVITY_LAUNCH_DELAY = 1500;
	
	private TextView lblConnectionStatus;		
	private Intent selectServerIntent;
	private Intent browseLibraryIntent;
	private LocalBroadcastManager localBroadcastManager;

	private final BroadcastReceiver buildSessionConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			handleBuildStatusChange(intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1));
		}
	};
	
	/*
	 * Returns true if the session needs to be restored,
	 * false if it doesn't
	 */
	public static boolean restoreSessionConnection(final Activity activity) {
		// Check to see that a URL can still be built
		if (SessionConnection.isBuilt()) return false;
		
		final Intent intent = new Intent(activity, InstantiateSessionConnectionActivity.class);
		intent.setAction(START_ACTIVITY_FOR_RETURN);
		activity.startActivityForResult(intent, ACTIVITY_ID);
		
		return true;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_status);
		
		lblConnectionStatus = (TextView)findViewById(R.id.lblConnectionStatus);		
		selectServerIntent = new Intent(this, ApplicationSettingsActivity.class);

		browseLibraryIntent = new Intent(this, BrowseLibraryActivity.class);
		browseLibraryIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

		localBroadcastManager = LocalBroadcastManager.getInstance(this);

		localBroadcastManager.registerReceiver(buildSessionConnectionReceiver, new IntentFilter(SessionConnection.buildSessionBroadcast));

		handleBuildStatusChange(SessionConnection.build(this));
	}
	
	private void handleBuildStatusChange(int status) {
		if (SessionConnection.completeConditions.contains(status))
			localBroadcastManager.unregisterReceiver(buildSessionConnectionReceiver);

		switch (status) {
		case BuildingSessionConnectionStatus.GettingLibrary:
			lblConnectionStatus.setText(R.string.lbl_getting_library_details);
			return;
		case BuildingSessionConnectionStatus.GettingLibraryFailed:
			lblConnectionStatus.setText(R.string.lbl_please_connect_to_valid_server);
			launchActivityDelayed(selectServerIntent);
			return;
		case BuildingSessionConnectionStatus.BuildingConnection:
			lblConnectionStatus.setText(R.string.lbl_connecting_to_server_library);
			return;
		case BuildingSessionConnectionStatus.BuildingConnectionFailed:
			lblConnectionStatus.setText(R.string.lbl_error_connecting_try_again);
			launchActivityDelayed(selectServerIntent);
			return;
		case BuildingSessionConnectionStatus.GettingView:
			lblConnectionStatus.setText(R.string.lbl_getting_library_views);
			return;
		case BuildingSessionConnectionStatus.GettingViewFailed:
			lblConnectionStatus.setText(R.string.lbl_library_no_views);
			launchActivityDelayed(selectServerIntent);
			return;
		case BuildingSessionConnectionStatus.BuildingSessionComplete:
			lblConnectionStatus.setText(R.string.lbl_connected);
			if (getIntent() == null || !START_ACTIVITY_FOR_RETURN.equals(getIntent().getAction()))
				launchActivityDelayed(browseLibraryIntent);
			else
				finish();
		}
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
