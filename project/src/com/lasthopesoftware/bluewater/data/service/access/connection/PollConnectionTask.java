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
	
	private static CopyOnWriteArraySet<OnStartListener<String, Void, Void>> mUniqueOnStartListeners = new CopyOnWriteArraySet<ISimpleTask.OnStartListener<String, Void, Void>>();
	private static CopyOnWriteArraySet<OnCompleteListener<String, Void, Void>> mUniqueOnCompleteListener = new CopyOnWriteArraySet<ISimpleTask.OnCompleteListener<String, Void, Void>>();
	private static CopyOnWriteArraySet<OnCancelListener<String, Void, Void>> mUniqueOnCancelListeners = new CopyOnWriteArraySet<ISimpleTask.OnCancelListener<String, Void, Void>>();
	private CopyOnWriteArraySet<OnErrorListener<String, Void, Void>> mUniqueOnErrorListeners = new CopyOnWriteArraySet<ISimpleTask.OnErrorListener<String, Void, Void>>();
	
	private PollConnectionTask(Context context) {
		synchronized (syncObj) {
			mContext = context;
			
			mTask = new SimpleTask<String, Void, Void>();
			mTask.setOnExecuteListener(this);
			
			mTask.addOnCompleteListener(new OnCompleteListener<String, Void, Void>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, Void> owner, Void result) {
					synchronized (syncObj) {
						for (OnCompleteListener<String, Void, Void> onCompleteListener : mUniqueOnCompleteListener)
							onCompleteListener.onComplete(owner, result);
						
						mUniqueOnCompleteListener.clear();
						mUniqueOnCancelListeners.clear();
					}
				}
			});
			
			mTask.addOnCancelListener(new OnCancelListener<String, Void, Void>() {
				
				@Override
				public void onCancel(ISimpleTask<String, Void, Void> owner, Void result) {
					synchronized (syncObj) {
						for (OnCancelListener<String, Void, Void> onCancelListener : mUniqueOnCancelListeners)
							onCancelListener.onCancel(owner, result);
						
						mUniqueOnCompleteListener.clear();
						mUniqueOnCancelListeners.clear();
					}
				}
			});
			
			for (OnStartListener<String, Void, Void> onStartListener : mUniqueOnStartListeners)
				mTask.addOnStartListener(onStartListener);
		}
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
//
//	public Boolean getResult() throws ExecutionException, InterruptedException {
//		return mTask.getResult();
//	}

	public LinkedList<Exception> getExceptions() {
		return mTask.getExceptions();
	}

	public SimpleTaskState getState() {
		return mTask.getState();
	}

	
	/* Differs from the normal on start listener in that it uses a static list that will be re-populated when a new Poll Connection task starts.
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener)
	 */
	public void addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<String, Void, Void> listener) {
		if (mUniqueOnStartListeners.add(listener))
			mTask.addOnStartListener(listener);
	}

	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is run
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener)
	 */
	public void addOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Void> listener) {
		mUniqueOnCompleteListener.add(listener);
	}
	
	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is run
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener)
	 */
	public void addOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<String, Void, Void> listener) {
		mUniqueOnCancelListeners.add(listener);
	}

	public void addOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Void> listener) {
		if (mUniqueOnErrorListeners.add(listener))
			mTask.addOnErrorListener(listener);
	}

	public void removeOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<String, Void, Void> listener) {
		if (mUniqueOnStartListeners.remove(listener))
			mTask.removeOnStartListener(listener);
	}

	public void removeOnCompleteListenerFromQueue(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Void> listener) {
		mUniqueOnCompleteListener.remove(listener);
	}

	public void removeOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Void> listener) {
		if (mUniqueOnErrorListeners.remove(listener))
			mTask.removeOnErrorListener(listener);
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