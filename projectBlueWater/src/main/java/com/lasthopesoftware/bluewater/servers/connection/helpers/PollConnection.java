package com.lasthopesoftware.bluewater.servers.connection.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PollConnection {
	
	private static final ExecutorService pollService = Executors.newSingleThreadExecutor(); 
	
	private final AsyncTask<String, Void, Void> mTask;
	private final Context mContext;
	private int mSleepTime = 2250;
	private int mConnectionTime = 2000;
	
	private final AtomicBoolean mIsConnectionRestored = new AtomicBoolean();
	private final AtomicBoolean mIsRefreshing = new AtomicBoolean();
	
	private static final HashSet<OnConnectionLostListener> mUniqueOnConnectionLostListeners = new HashSet<>();
	private final HashSet<OnConnectionRegainedListener> mUniqueOnConnectionRegainedListeners = new HashSet<>();
	private final HashSet<OnPollingCancelledListener> mUniqueOnCancelListeners = new HashSet<>();
	
	private PollConnection(Context context) {
		mContext = context;
		
		mTask = new AsyncTask<String, Void, Void>() {

			@Override
			protected void onPreExecute() {
				synchronized (mUniqueOnConnectionLostListeners) {
					for (OnConnectionLostListener onConnectionLostListener : mUniqueOnConnectionLostListeners) onConnectionLostListener.onConnectionLost();
				}
			}
			
			@Override
			protected Void doInBackground(String... params) {
				// Don't use timeout since if it can't resolve a host it will throw an exception immediately
				// TODO need a blocking refresh configuration (that throws an error when run on a UI thread) for this one scenario

				while (!isCancelled() && !mIsConnectionRestored.get()) {
					
					try {
						Thread.sleep(mSleepTime);
						// Also arbitrarily increase the sleep time slowly up 32000 ms
						if (mSleepTime < 30000) mSleepTime *= 1.5;
					} catch (InterruptedException ie) {
						return null;
					}
					
					if (isCancelled()) return null;
					
					if (mIsRefreshing.get()) continue;
					mIsRefreshing.set(true);

					final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);

					final BroadcastReceiver buildSessionBroadcastReceiver = new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							final int buildStatus = intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1);

							if (!SessionConnection.completeConditions.contains(buildStatus)) return;

							mIsRefreshing.set(false);
							localBroadcastManager.unregisterReceiver(this);

							mIsConnectionRestored.set(buildStatus == SessionConnection.BuildingSessionConnectionStatus.BuildingSessionComplete);
						}
					};

					final BroadcastReceiver refreshBroadcastReceiver = new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							localBroadcastManager.unregisterReceiver(this);

							// Build the connect time up to 32 seconds
							if (mConnectionTime < 32000) mConnectionTime *= 2;

							final boolean isRefreshSuccessful = intent.getBooleanExtra(SessionConnection.isRefreshSuccessfulStatus, false);
							if (!isRefreshSuccessful) return;

							localBroadcastManager.unregisterReceiver(buildSessionBroadcastReceiver);

							mIsRefreshing.set(false);
							mIsConnectionRestored.set(true);
						}
					};

					localBroadcastManager.registerReceiver(buildSessionBroadcastReceiver, new IntentFilter(SessionConnection.buildSessionBroadcast));
					localBroadcastManager.registerReceiver(refreshBroadcastReceiver, new IntentFilter(SessionConnection.refreshSessionBroadcast));

					SessionConnection.refresh(mContext);
				}

				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				for (OnConnectionRegainedListener onConnectionRegainedListener : mUniqueOnConnectionRegainedListeners) onConnectionRegainedListener.onConnectionRegained();

				// Let on cancelled clear the completed listeners
				if (!isCancelled())
					clearCompleteListeners();
			}
			
			@Override
			protected void onCancelled(Void result) {
				for (OnPollingCancelledListener onCancelListener : mUniqueOnCancelListeners) onCancelListener.onPollingCancelled();
				
				clearCompleteListeners();
			}
		};
	}
	
	private void clearCompleteListeners() {
		mUniqueOnConnectionRegainedListeners.clear();
		mUniqueOnCancelListeners.clear();
	}
	
	public synchronized void startPolling() {
		if (mTask.getStatus() == AsyncTask.Status.PENDING) mTask.executeOnExecutor(pollService);
	}
	
	public synchronized void stopPolling() {
		mTask.cancel(true);
	}
	
	public boolean isRunning() {
		return mTask.getStatus() == AsyncTask.Status.RUNNING;
	}
	
	public synchronized boolean isFinished() {
		return mTask.getStatus() == AsyncTask.Status.FINISHED;
	}
	
	/* Differs from the normal on start listener in that it uses a static list that will be re-populated when a new Poll Connection task starts.
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener)
	 */
	public void addOnConnectionLostListener(OnConnectionLostListener listener) {
		synchronized(mUniqueOnConnectionLostListeners) {
			mUniqueOnConnectionLostListeners.add(listener);
		}
	}

	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is run
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener)
	 */
	public void addOnConnectionRegainedListener(OnConnectionRegainedListener listener) {
		synchronized(mUniqueOnConnectionRegainedListeners) {
			mUniqueOnConnectionRegainedListeners.add(listener);
		}
	}
	
	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is run
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener)
	 */
	public void addOnPollingCancelledListener(OnPollingCancelledListener listener) {
		synchronized(mUniqueOnCancelListeners) {
			mUniqueOnCancelListeners.add(listener);
		}
	}

	public void removeOnConnectionLostListener(OnConnectionLostListener listener) {
		synchronized(mUniqueOnConnectionLostListeners) {
			mUniqueOnConnectionLostListeners.remove(listener);
		}
	}

	public void removeOnConnectionRegainedListener(OnConnectionRegainedListener listener) {
		synchronized(mUniqueOnConnectionRegainedListeners) {
			mUniqueOnConnectionRegainedListeners.remove(listener);
		}
	}
	
	public void removeOnPollingCancelledListener(OnPollingCancelledListener listener) {
		synchronized(mUniqueOnCancelListeners) {
			mUniqueOnCancelListeners.remove(listener);
		}
	}
	
	public interface OnConnectionLostListener {
		void onConnectionLost();
	}
	
	public interface OnConnectionRegainedListener {
		void onConnectionRegained();
	}
	
	public interface OnPollingCancelledListener {
		void onPollingCancelled();
	}
	
	public static class Instance {
		private static PollConnection _instance = null;
		
		public static synchronized PollConnection get(Context context) {
			if (_instance == null || _instance.isFinished()) _instance = new PollConnection(context);
			return _instance;
		}
	}
}