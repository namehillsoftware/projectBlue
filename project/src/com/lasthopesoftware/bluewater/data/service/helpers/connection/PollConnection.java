package com.lasthopesoftware.bluewater.data.service.helpers.connection;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class PollConnection {
	
	private static final ExecutorService pollService = Executors.newSingleThreadExecutor(); 
	
	private final AsyncTask<String, Void, Void> mTask;
	private final Context mContext;
	private static final int mSleepTime = 2000;
	private int mConnectionTime = 2000;
	
	private final AtomicBoolean mIsConnectionRestored = new AtomicBoolean();
	private final AtomicBoolean mIsRefreshing = new AtomicBoolean();
	
	private static final HashSet<OnConnectionLostListener> mUniqueOnConnectionLostListeners = new HashSet<OnConnectionLostListener>();
	private final HashSet<OnConnectionRegainedListener> mUniqueOnConnectionRegainedListeners = new HashSet<OnConnectionRegainedListener>();
	private final HashSet<OnPollingCancelledListener> mUniqueOnCancelListeners = new HashSet<OnPollingCancelledListener>();
	
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
					} catch (InterruptedException ie) {
						return null;
					}
					
					if (isCancelled()) return null;
					
					if (!mIsRefreshing.get()) {
						mIsRefreshing.set(true);
						ConnectionManager.refreshConfiguration(mContext, new OnCompleteListener<Integer, Void, Boolean>() {
			
							@Override
							public void onComplete(ISimpleTask<Integer, Void, Boolean> owner, Boolean result) {
								mIsRefreshing.set(false);
								if (result == Boolean.TRUE) mIsConnectionRestored.set(true);
								// Build the connect time up to 32 seconds
								if (mConnectionTime < 32000) mConnectionTime *= 2;	
							}
							
						});
					}
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				
				for (OnConnectionRegainedListener onConnectionRegainedListener : mUniqueOnConnectionRegainedListeners) onConnectionRegainedListener.onConnectionRegained();
				
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
		if (mTask.getStatus() != AsyncTask.Status.RUNNING) mTask.executeOnExecutor(pollService);
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