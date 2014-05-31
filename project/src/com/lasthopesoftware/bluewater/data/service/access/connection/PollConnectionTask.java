package com.lasthopesoftware.bluewater.data.service.access.connection;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;

import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnProgressListener;
import com.lasthopesoftware.threading.ISimpleTask.OnStartListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class PollConnectionTask implements OnExecuteListener<String, Void, Boolean> {
	
	private volatile boolean mStopWaitingForConnection = false;
	private SimpleTask<String, Void, Boolean> mTask;
	private Context mContext;
	private int mSleepTime = 2000;
	
	private static Object syncObj = new Object();
	
	private static CopyOnWriteArraySet<OnStartListener<String, Void, Boolean>> mUniqueOnStartListeners = new CopyOnWriteArraySet<ISimpleTask.OnStartListener<String, Void, Boolean>>();
	private static CopyOnWriteArraySet<OnCompleteListener<String, Void, Boolean>> mUniqueOnCompleteListener = new CopyOnWriteArraySet<ISimpleTask.OnCompleteListener<String, Void, Boolean>>();
	private CopyOnWriteArraySet<OnErrorListener<String, Void, Boolean>> mUniqueOnErrorListeners = new CopyOnWriteArraySet<ISimpleTask.OnErrorListener<String,Void,Boolean>>();
	
	private PollConnectionTask(Context context) {
		synchronized (syncObj) {
			mContext = context;
			
			mTask = new SimpleTask<String, Void, Boolean>();
			mTask.setOnExecuteListener(this);
			
			mTask.addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
					synchronized (syncObj) {
						for (OnCompleteListener<String, Void, Boolean> onCompleteListener : mUniqueOnCompleteListener)
							onCompleteListener.onComplete(owner, result);
						
						mUniqueOnCompleteListener.clear();
					}
				}
			});
			
			for (OnStartListener<String, Void, Boolean> onStartListener : mUniqueOnStartListeners)
				mTask.addOnStartListener(onStartListener);
		}
	}

	@Override
	public Boolean onExecute(ISimpleTask<String, Void, Boolean> owner, String... params) throws Exception {
		// Don't use timeout since if it can't resolve a host it will throw an exception immediately
		while (!ConnectionManager.refreshConfiguration(mContext)) {
			// Build the wait time up to 32 seconds
			try {
				Thread.sleep(mSleepTime);
			} catch (InterruptedException ie) {
				return Boolean.FALSE;
			}
			if (mStopWaitingForConnection) return Boolean.FALSE;
			
			if (mSleepTime < 32000) mSleepTime *= 2;			
		}
		
		return Boolean.TRUE;
	}
	
	public void startPolling() {
		synchronized (syncObj) {
			if (mTask.getStatus() != AsyncTask.Status.RUNNING) mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}
	
	public void stopPolling() {
		mStopWaitingForConnection = true;
	}
	
	public boolean isRunning() {
		return mTask.getStatus() == Status.RUNNING;
	}
	
	public boolean isFinished() {
		return mTask.getState() != SimpleTaskState.INITIALIZED && mTask.getState() != SimpleTaskState.EXECUTING;
	}

	public Boolean getResult() throws ExecutionException, InterruptedException {
		return mTask.getResult();
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
	public void addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<String, Void, Boolean> listener) {
		if (mUniqueOnStartListeners.add(listener))
			mTask.addOnStartListener(listener);
	}

	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is run
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener)
	 */
	public void addOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Boolean> listener) {
		if (!mUniqueOnCompleteListener.contains(listener))
			mUniqueOnCompleteListener.add(listener);
	}

	public void addOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Boolean> listener) {
		if (mUniqueOnErrorListeners.add(listener))
			mTask.addOnErrorListener(listener);
	}

	public void removeOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<String, Void, Boolean> listener) {
		if (mUniqueOnStartListeners.remove(listener))
			mTask.removeOnStartListener(listener);
	}

	public void removeOnCompleteListenerFromQueue(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Boolean> listener) {
		mUniqueOnCompleteListener.remove(listener);
	}

	public void removeOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Boolean> listener) {
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