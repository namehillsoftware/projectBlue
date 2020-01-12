package com.lasthopesoftware.bluewater.client.connection.session;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.Lazy;

public class InstantiateSessionConnectionActivity extends Activity {
	
	public static final int ACTIVITY_ID = 2032;
	
	private static final String START_ACTIVITY_FOR_RETURN = MagicPropertyBuilder.buildMagicPropertyName(InstantiateSessionConnectionActivity.class, "START_ACTIVITY_FOR_RETURN");
	
	private static final int ACTIVITY_LAUNCH_DELAY = 1500;
	
	private LazyViewFinder<TextView> lblConnectionStatus = new LazyViewFinder<>(this, R.id.lblConnectionStatus);
	private final Lazy<Intent> selectServerIntent = new Lazy<Intent>(() -> new Intent(this, ApplicationSettingsActivity.class));
	private final AbstractSynchronousLazy<Intent> browseLibraryIntent = new AbstractSynchronousLazy<Intent>() {
		@Override
		protected Intent create() {
			final Intent browseLibraryIntent = new Intent(InstantiateSessionConnectionActivity.this, BrowserEntryActivity.class);
			browseLibraryIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			return browseLibraryIntent;
		}
	};

	private final Lazy<LocalBroadcastManager> localBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(this));

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
	public static Promise<Boolean> restoreSessionConnection(final Activity activity) {
		return SessionConnection.getInstance(activity).promiseSessionConnection()
			.eventually(LoopedInPromise.response(c -> {
				if (c != null) return false;

				final Intent intent = new Intent(activity, InstantiateSessionConnectionActivity.class);
				intent.setAction(START_ACTIVITY_FOR_RETURN);
				activity.startActivityForResult(intent, ACTIVITY_ID);

				return true;
			}, activity));
	}

	public static void startNewConnection(final Context context) {
		context.startActivity(new Intent(context, InstantiateSessionConnectionActivity.class));
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_status);

		localBroadcastManager.getObject().registerReceiver(buildSessionConnectionReceiver, new IntentFilter(SessionConnection.buildSessionBroadcast));

		SessionConnection.getInstance(this)
			.promiseSessionConnection()
			.eventually(LoopedInPromise.response(c -> {
				if (c == null)
					launchActivityDelayed(selectServerIntent.getObject());
				else if (getIntent() == null || !START_ACTIVITY_FOR_RETURN.equals(getIntent().getAction()))
					launchActivityDelayed(browseLibraryIntent.getObject());
				else
					finish();

				localBroadcastManager.getObject().unregisterReceiver(buildSessionConnectionReceiver);
				return Promise.empty();
			}, this), LoopedInPromise.response(e-> {
				launchActivityDelayed(selectServerIntent.getObject());
				localBroadcastManager.getObject().unregisterReceiver(buildSessionConnectionReceiver);
				return null;
			}, this));
	}
	
	private void handleBuildStatusChange(int status) {
		final TextView lblConnectionStatusView = lblConnectionStatus.findView();
		switch (status) {
		case BuildingSessionConnectionStatus.GettingLibrary:
			lblConnectionStatusView.setText(R.string.lbl_getting_library_details);
			return;
		case BuildingSessionConnectionStatus.GettingLibraryFailed:
			lblConnectionStatusView.setText(R.string.lbl_please_connect_to_valid_server);
			return;
		case BuildingSessionConnectionStatus.BuildingConnection:
			lblConnectionStatusView.setText(R.string.lbl_connecting_to_server_library);
			return;
		case BuildingSessionConnectionStatus.BuildingConnectionFailed:
			lblConnectionStatusView.setText(R.string.lbl_error_connecting_try_again);
			return;
		case BuildingSessionConnectionStatus.BuildingSessionComplete:
			lblConnectionStatusView.setText(R.string.lbl_connected);
		}
	}
	
	private void launchActivityDelayed(Intent intent) {
		final Handler handler = new Handler();
		handler.postDelayed(new LaunchRunnable(this, intent), ACTIVITY_LAUNCH_DELAY);
	}
	
	private static class LaunchRunnable implements Runnable {
		private final Intent intent;
		private final Context context;
		
		public LaunchRunnable(final Context context, final Intent intent) {
			this.intent = intent;
			this.context = context;
		}
		
		@Override
		public void run() {
			context.startActivity(intent);
		}
		
	}
}
