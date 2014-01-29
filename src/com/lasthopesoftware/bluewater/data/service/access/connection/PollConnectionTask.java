package com.lasthopesoftware.bluewater.data.service.access.connection;

import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;

import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class PollConnectionTask implements ISimpleTask<String, Void, Boolean>, OnExecuteListener<String, Void, Boolean> {
	
	private volatile boolean mStopWaitingForConnection = false;
	private SimpleTask<String, Void, Boolean> mTask;
	
	private static CopyOnWriteArraySet<OnStartListener<String, Void, Boolean>> mUniqueOnStartListeners = new CopyOnWriteArraySet<ISimpleTask.OnStartListener<String, Void, Boolean>>();
	private CopyOnWriteArraySet<OnExecuteListener<String, Void, Boolean>> mUniqueOnExecuteListeners = new CopyOnWriteArraySet<ISimpleTask.OnExecuteListener<String, Void, Boolean>>();
	private CopyOnWriteArraySet<OnCompleteListener<String, Void, Boolean>> mUniqueOnCompleteListeners = new CopyOnWriteArraySet<ISimpleTask.OnCompleteListener<String, Void, Boolean>>();
	private CopyOnWriteArraySet<OnProgressListener<String, Void, Boolean>> mUniqueOnProgressListeners = new CopyOnWriteArraySet<ISimpleTask.OnProgressListener<String, Void, Boolean>>();
	private CopyOnWriteArraySet<OnErrorListener<String, Void, Boolean>> mUniqueOnErrorListeners = new CopyOnWriteArraySet<ISimpleTask.OnErrorListener<String,Void,Boolean>>(); 
	
	private PollConnectionTask() {
		mTask = new SimpleTask<String, Void, Boolean>();
		mTask.addOnExecuteListener(this);
		
		for (OnStartListener<String, Void, Boolean> onStartListener : mUniqueOnStartListeners)
			mTask.addOnStartListener(onStartListener);
	}

	@Override
	public void onExecute(ISimpleTask<String, Void, Boolean> owner, String... params) throws Exception {
		while (!JrTestConnection.doTest()) {
			try {
				Thread.sleep(3000);
				if (mStopWaitingForConnection) {
					
					owner.setResult(Boolean.FALSE);
					return;
				}
			} catch (InterruptedException e) {
				LoggerFactory.getLogger(PollConnectionTask.class).warn("Poll Connection Task interrupted.", e);
				owner.setResult(Boolean.FALSE);
				return;
			}
		}
		
		owner.setResult(Boolean.TRUE);
	}
	
	public synchronized void startPolling() {
		if (mTask.getStatus() != Status.RUNNING) mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	public void stopPolling() {
		mStopWaitingForConnection = true;
	}
	
	public boolean isRunning() {
		return mTask.getStatus() == Status.RUNNING;
	}
	
	public boolean isFinished() {
		return mTask.getStatus() == Status.FINISHED;
	}

	@Override
	public Boolean getResult() throws ExecutionException, InterruptedException {
		return mTask.getResult();
	}

	@Override
	public void setResult(Boolean result) {
		mTask.setResult(result);
	}

	@Override
	public LinkedList<Exception> getExceptions() {
		return mTask.getExceptions();
	}

	@Override
	public SimpleTaskState getState() {
		return mTask.getState();
	}

	
	/* Differs from the normal on start listener in that it uses a static list that will be re-populated when a new Poll Connection task starts.
	 * @see com.lasthopesoftware.threading.ISimpleTask#addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener)
	 */
	@Override
	public void addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<String, Void, Boolean> listener) {
		if (mUniqueOnStartListeners.add(listener))
			mTask.addOnStartListener(listener);
	}

	@Override
	public void addOnExecuteListener(com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener<String, Void, Boolean> listener) {
		if (mUniqueOnExecuteListeners.add(listener))
			mTask.addOnExecuteListener(listener);
	}

	@Override
	public void addOnProgressListener(com.lasthopesoftware.threading.ISimpleTask.OnProgressListener<String, Void, Boolean> listener) {
		if (mUniqueOnProgressListeners.add(listener))
			mTask.addOnProgressListener(listener);
	}

	@Override
	public void addOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Boolean> listener) {
		if (isFinished()) {
			try {
				listener.onComplete(this, getResult());
			} catch (ExecutionException e) {
				LoggerFactory.getLogger(PollConnectionTask.class).error(e.toString(), e);
			} catch (InterruptedException e) {
				LoggerFactory.getLogger(PollConnectionTask.class).error(e.toString(), e);
			}
			return;
		}
		
		if (mUniqueOnCompleteListeners.add(listener))
			mTask.addOnCompleteListener(listener);
	}

	@Override
	public void addOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Boolean> listener) {
		if (mUniqueOnErrorListeners.add(listener))
			mTask.addOnErrorListener(listener);
	}

	@Override
	public void removeOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<String, Void, Boolean> listener) {
		if (mUniqueOnStartListeners.remove(listener))
			mTask.removeOnStartListener(listener);
	}

	@Override
	public void removeOnExecuteListener(com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener<String, Void, Boolean> listener) {
		if (mUniqueOnExecuteListeners.remove(listener))
			mTask.removeOnExecuteListener(listener);
	}

	@Override
	public void removeOnProgressListener(com.lasthopesoftware.threading.ISimpleTask.OnProgressListener<String, Void, Boolean> listener) {
		if (mUniqueOnProgressListeners.remove(listener))
			mTask.removeOnProgressListener(listener);
	}

	@Override
	public void removeOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Boolean> listener) {
		if (mUniqueOnCompleteListeners.remove(listener))
			mTask.removeOnCompleteListener(listener);
	}

	@Override
	public void removeOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Boolean> listener) {
		if (mUniqueOnErrorListeners.remove(listener))
			mTask.removeOnErrorListener(listener);
	}
	
	public static class Instance {
		private static PollConnectionTask _instance = null;
		
		public synchronized static PollConnectionTask get() {
			if (_instance == null || _instance.isFinished()) _instance = new PollConnectionTask();
			return _instance;
		}
	}
}