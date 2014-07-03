package com.lasthopesoftware.bluewater.data.service.access.connection;

import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArraySet;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCancelListener;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnStartListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class PollConnectionTask implements OnExecuteListener<String, Void, Void> {
	
	private SimpleTask<String, Void, Void> mTask;
	private Context mContext;
	private int mSleepTime = 2000;
	
	private static Object syncObj = new Object();
	
	private static CopyOnWriteArraySet<OnConnectionLostListener> mUniqueOnConnectionLostListeners = new CopyOnWriteArraySet<OnConnectionLostListener>();
	private static CopyOnWriteArraySet<OnConnectionRegainedListener> mUniqueOnConnectionRegainedListener = new CopyOnWriteArraySet<OnConnectionRegainedListener>();
	private static CopyOnWriteArraySet<OnPollingCancelledListener> mUniqueOnCancelListeners = new CopyOnWriteArraySet<OnPollingCancelledListener>();
	private CopyOnWriteArraySet<OnErrorListener<String, Void, Void>> mUniqueOnErrorListeners = new CopyOnWriteArraySet<ISimpleTask.OnErrorListener<String, Void, Void>>();
	
	private PollConnectionTask(Context context) {
		synchronized (syncObj) {
			mContext = context;
			
			mTask = new SimpleTask<String, Void, Void>();
			mTask.setOnExecuteListener(this);
			
			mTask.addOnStartListener(new OnStartListener<String, Void, Void>() {
				
				@Override
				public void onStart(ISimpleTask<String, Void, Void> owner) {
					for (OnConnectionLostListener onConnectionLostListener : mUniqueOnConnectionLostListeners) onConnectionLostListener.onConnectionLost();
				}
			});
			
			mTask.addOnCompleteListener(new OnCompleteListener<String, Void, Void>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, Void> owner, Void result) {
					synchronized (syncObj) {
						for (OnConnectionRegainedListener onConnectionRegainedListener : mUniqueOnConnectionRegainedListener) onConnectionRegainedListener.onConnectionRegained();
						
						clearCompleteListeners();
					}
				}
			});
			
			mTask.addOnCancelListener(new OnCancelListener<String, Void, Void>() {
				
				@Override
				public void onCancel(ISimpleTask<String, Void, Void> owner, Void result) {
					synchronized (syncObj) {
						for (OnPollingCancelledListener onCancelListener : mUniqueOnCancelListeners) onCancelListener.onPollingCancelled();
						
						clearCompleteListeners();
					}
				}
			});
		}
	}
	
	private void clearCompleteListeners() {
		mUniqueOnConnectionRegainedListener.clear();
		mUniqueOnCancelListeners.clear();
	}

	@Override
	public Void onExecute(ISimpleTask<String, Void, Void> owner, String... params) throws Exception {
		// Don't use timeout since if it can't resolve a host it will throw an exception immediately
		while (!ConnectionManager.refreshConfiguration(mContext)) {
			// Build the wait time up to 32 seconds
			try {
				Thread.sleep(mSleepTime);
			} catch (InterruptedException ie) {
				return null;
			}
						
			if (mSleepTime < 32000) mSleepTime *= 2;			
		}
		
		return null;
	}
	
	public void startPolling() {
		synchronized (syncObj) {
			if (mTask.getStatus() != AsyncTask.Status.RUNNING) mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
		mUniqueOnConnectionLostListeners.add(listener);
	}

	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is run
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener)
	 */
	public void addOnConnectionRegainedListener(OnConnectionRegainedListener listener) {
		mUniqueOnConnectionRegainedListener.add(listener);
	}
	
	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is run
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener)
	 */
	public void addOnPollingCancelledListener(OnPollingCancelledListener listener) {
		mUniqueOnCancelListeners.add(listener);
	}

	public void addOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Void> listener) {
		if (mUniqueOnErrorListeners.add(listener))
			mTask.addOnErrorListener(listener);
	}

	public void removeOnConnectionLostListener(OnConnectionLostListener listener) {
		mUniqueOnConnectionLostListeners.remove(listener);
	}

	public void removeOnConnectionRegainedListener(OnConnectionRegainedListener listener) {
		mUniqueOnConnectionRegainedListener.remove(listener);
	}
	
	public void removeOnPollingCancelledListener(OnPollingCancelledListener listener) {
		mUniqueOnCancelListeners.remove(listener);
	}

	public void removeOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Void> listener) {
		if (mUniqueOnErrorListeners.remove(listener))
			mTask.removeOnErrorListener(listener);
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
		private static volatile PollConnectionTask _instance = null;
		private static Object syncObj = new Object();
		
		public static PollConnectionTask get(Context context) {
			synchronized (syncObj) {
				if (_instance == null || _instance.isFinished()) _instance = new PollConnectionTask(context);
				return _instance;
			}
		}
	}
}