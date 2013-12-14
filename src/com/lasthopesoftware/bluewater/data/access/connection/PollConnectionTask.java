package com.lasthopesoftware.bluewater.data.access.connection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class PollConnectionTask implements ISimpleTask<String, Void, Boolean>, OnExecuteListener<String, Void, Boolean> {
	
	private volatile boolean mStopWaitingForConnection = false;
	private SimpleTask<String, Void, Boolean> mTask;
	
	private HashSet<OnStartListener<String, Void, Boolean>> mUniqueOnStartListeners = new HashSet<ISimpleTask.OnStartListener<String, Void, Boolean>>();
	private HashSet<OnExecuteListener<String, Void, Boolean>> mUniqueOnExecuteListeners = new HashSet<ISimpleTask.OnExecuteListener<String, Void, Boolean>>();
	private HashSet<OnCompleteListener<String, Void, Boolean>> mUniqueOnCompleteListeners = new HashSet<ISimpleTask.OnCompleteListener<String, Void, Boolean>>();
	private HashSet<OnProgressListener<String, Void, Boolean>> mUniqueOnProgressListeners = new HashSet<ISimpleTask.OnProgressListener<String, Void, Boolean>>();
	private HashSet<OnErrorListener<String, Void, Boolean>> mUniqueOnErrorListeners = new HashSet<ISimpleTask.OnErrorListener<String,Void,Boolean>>(); 
	
	private PollConnectionTask() {
		mTask = new SimpleTask<String, Void, Boolean>();
		mTask.addOnExecuteListener(this);
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
				e.printStackTrace();
				owner.setResult(Boolean.FALSE);
				return;
			}
		}
		
		owner.setResult(Boolean.TRUE);
	}
	
	public void startPolling() {
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		mUniqueOnStartListeners.remove(listener);
		mTask.removeOnStartListener(listener);
	}

	@Override
	public void removeOnExecuteListener(com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener<String, Void, Boolean> listener) {
		mUniqueOnExecuteListeners.remove(listener);
		mTask.removeOnExecuteListener(listener);
	}

	@Override
	public void removeOnProgressListener(com.lasthopesoftware.threading.ISimpleTask.OnProgressListener<String, Void, Boolean> listener) {
		mUniqueOnProgressListeners.remove(listener);
		mTask.removeOnProgressListener(listener);
	}

	@Override
	public void removeOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Boolean> listener) {
		mUniqueOnCompleteListeners.remove(listener);
		mTask.removeOnCompleteListener(listener);
	}

	@Override
	public void removeOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Boolean> listener) {
		mUniqueOnErrorListeners.remove(listener);
		mTask.removeOnErrorListener(listener);
	}
	
	public static class Instance {
		private static Object mSyncObject = new Object();
		private static PollConnectionTask _instance = null;
		
		public static PollConnectionTask get() {
			synchronized(mSyncObject) {
				if (_instance == null || _instance.isFinished()) _instance = new PollConnectionTask();
				return _instance;
			}
		}
	}
}