package com.lasthopesoftware.bluewater.data.access.connection;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.os.AsyncTask;

import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTaskState;

public class PollConnectionTask implements ISimpleTask<String, Void, Boolean>, OnExecuteListener<String, Void, Boolean> {
	
	private boolean mStopWaitingForConnection = false;
	private SimpleTask<String, Void, Boolean> mTask;
	
	public PollConnectionTask() {
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
		mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	public void stopPolling() {
		mStopWaitingForConnection = true;
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
		mTask.addOnStartListener(listener);
	}

	@Override
	public void addOnExecuteListener(com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener<String, Void, Boolean> listener) {
		mTask.addOnExecuteListener(listener);
	}

	@Override
	public void addOnProgressListener(com.lasthopesoftware.threading.ISimpleTask.OnProgressListener<String, Void, Boolean> listener) {
		mTask.addOnProgressListener(listener);
	}

	@Override
	public void addOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Boolean> listener) {
		mTask.addOnCompleteListener(listener);
	}

	@Override
	public void addOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Boolean> listener) {
		mTask.addOnErrorListener(listener);
	}

	@Override
	public void removeOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<String, Void, Boolean> listener) {
		mTask.removeOnStartListener(listener);
	}

	@Override
	public void removeOnExecuteListener(com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener<String, Void, Boolean> listener) {
		mTask.removeOnExecuteListener(listener);
	}

	@Override
	public void removeOnProgressListener(com.lasthopesoftware.threading.ISimpleTask.OnProgressListener<String, Void, Boolean> listener) {
		mTask.removeOnProgressListener(listener);
	}

	@Override
	public void removeOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<String, Void, Boolean> listener) {
		mTask.removeOnCompleteListener(listener);
	}

	@Override
	public void removeOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Boolean> listener) {
		mTask.removeOnErrorListener(listener);
	}
}