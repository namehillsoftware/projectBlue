package com.lasthopesoftware.bluewater.data.service.helpers.connection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCancelListener;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnStartListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class PollConnection implements OnExecuteListener<String, Void, Void> {
	
	private static final ExecutorService pollService = Executors.newSingleThreadExecutor(); 
	
	private final SimpleTask<String, Void, Void> mTask;
	private final Context mContext;
	private static final int mSleepTime = 2000;
	private int mConnectionTime = 2000;
	
	private final AtomicBoolean mIsConnectionRestored = new AtomicBoolean();
	private final AtomicBoolean mIsRefreshing = new AtomicBoolean();
	
	private static final Object syncObj = new Object();
	
	private static final HashSet<OnConnectionLostListener> mUniqueOnConnectionLostListeners = new HashSet<OnConnectionLostListener>();
	private final HashSet<OnConnectionRegainedListener> mUniqueOnConnectionRegainedListeners = new HashSet<OnConnectionRegainedListener>();
	private final HashSet<OnPollingCancelledListener> mUniqueOnCancelListeners = new HashSet<OnPollingCancelledListener>();
	private final HashSet<OnErrorListener<String, Void, Void>> mUniqueOnErrorListeners = new HashSet<ISimpleTask.OnErrorListener<String, Void, Void>>();
	
	private PollConnection(Context context) {
		synchronized (syncObj) {
			mContext = context;
			
			mTask = new SimpleTask<String, Void, Void>();
			mTask.setOnExecuteListener(this);
			
			mTask.addOnStartListener(new OnStartListener<String, Void, Void>() {
				
				@Override
				public void onStart(ISimpleTask<String, Void, Void> owner) {
					synchronized (mUniqueOnConnectionLostListeners) {
						for (OnConnectionLostListener onConnectionLostListener : mUniqueOnConnectionLostListeners) onConnectionLostListener.onConnectionLost();
					}
				}
			});
			
			mTask.addOnCompleteListener(new OnCompleteListener<String, Void, Void>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, Void> owner, Void result) {
					for (OnConnectionRegainedListener onConnectionRegainedListener : mUniqueOnConnectionRegainedListeners) onConnectionRegainedListener.onConnectionRegained();
					
					clearCompleteListeners();
				}
			});
			
			mTask.addOnCancelListener(new OnCancelListener<String, Void, Void>() {
				
				@Override
				public void onCancel(ISimpleTask<String, Void, Void> owner, Void result) {
					for (OnPollingCancelledListener onCancelListener : mUniqueOnCancelListeners) onCancelListener.onPollingCancelled();
					
					clearCompleteListeners();
				}
			});
		}
	}
	
	private void clearCompleteListeners() {
		mUniqueOnConnectionRegainedListeners.clear();
		mUniqueOnCancelListeners.clear();
	}

	@Override
	public Void onExecute(ISimpleTask<String, Void, Void> owner, String... params) throws Exception {
		// Don't use timeout since if it can't resolve a host it will throw an exception immediately
		// TODO need a blocking refresh configuration (that throws an error when run on a UI thread) for this one scenario
		while (!mIsConnectionRestored.get()) {
			
			try {
				Thread.sleep(mSleepTime);
			} catch (InterruptedException ie) {
				return null;
			}
			
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
	
	public void startPolling() {
		synchronized (syncObj) {
			if (mTask.getStatus() != AsyncTask.Status.RUNNING) mTask.executeOnExecutor(pollService);
		}
	}
	
	public void stopPolling() {
		mTask.cancel(true);
	}
	
	public boolean isRunning() {
		return mTask.getStatus() == Status.RUNNING;
	}
	
	public boolean isFinished() {
		return mTask.getState() != SimpleTaskState.INITIALIZED && mTask.getState() != SimpleTaskState.EXECUTING;
	}

	public LinkedList<Exception> getExceptions() {
		return mTask.getExceptions();
	}

	public SimpleTaskState getState() {
		return mTask.getState();
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

	public void addOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Void> listener) {
		synchronized(mUniqueOnErrorListeners) {
			if (mUniqueOnErrorListeners.add(listener))
				mTask.addOnErrorListener(listener);
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

	public void removeOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Void> listener) {
		synchronized(mUniqueOnErrorListeners) {
			if (mUniqueOnErrorListeners.remove(listener))
				mTask.removeOnErrorListener(listener);
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
		private static volatile PollConnection _instance = null;
		private static Object syncObj = new Object();
		
		public static PollConnection get(Context context) {
			synchronized (syncObj) {
				if (_instance == null || _instance.isFinished()) _instance = new PollConnection(context);
				return _instance;
			}
		}
	}
}