package com.lasthopesoftware.threading;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;

public class SimpleTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> implements ISimpleTask<TParams, TProgress, TResult> {

	private TResult mResult;
	private SimpleTaskState mState = SimpleTaskState.INITIALIZED;
	
	LinkedList<OnExecuteListener<TParams, TProgress, TResult>> onExecuteListeners = new LinkedList<OnExecuteListener<TParams, TProgress, TResult>>();
	LinkedList<OnProgressListener<TParams, TProgress, TResult>> onProgressListeners = new LinkedList<OnProgressListener<TParams, TProgress, TResult>>();
	LinkedList<OnCompleteListener<TParams, TProgress, TResult>> onCompleteListeners = new LinkedList<OnCompleteListener<TParams, TProgress, TResult>>();
	LinkedList<OnStartListener<TParams, TProgress, TResult>> onStartListeners = new LinkedList<OnStartListener<TParams, TProgress, TResult>>();
	LinkedList<OnErrorListener<TParams, TProgress, TResult>> onErrorListeners = new LinkedList<OnErrorListener<TParams, TProgress, TResult>>();
		
	@Override
	protected void onPreExecute() {
		for (OnStartListener<TParams, TProgress, TResult> listener : onStartListeners) listener.onStart(this);
	}
	
	@Override
	protected TResult doInBackground(TParams... params) {
		mState = SimpleTaskState.SUCCESS;
		for (OnExecuteListener<TParams, TProgress, TResult> workEvent : onExecuteListeners) {
			try {
				workEvent.onExecute(this, params);
			} catch (Exception ex) {
				mState = SimpleTaskState.ERROR;
				boolean continueExecution = true;
				
				for (OnErrorListener<TParams, TProgress, TResult> errorListener : onErrorListeners) continueExecution &= errorListener.onError(this, ex);
				if (!continueExecution) break;
			}
		}
		return mResult;
	}
	
	@Override
	protected void onProgressUpdate(TProgress... values) {
		for (OnProgressListener<TParams, TProgress, TResult> progressListener : onProgressListeners) progressListener.onReportProgress(this, values);
	}
	
	@Override
	protected void onPostExecute(TResult result) {
		for (OnCompleteListener<TParams, TProgress, TResult> completeListener : onCompleteListeners) completeListener.onComplete(this, result);
	}
	
	@Override
	public void addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		onStartListeners.add(listener);
	}
	
	@Override
	public void addOnExecuteListener(OnExecuteListener<TParams, TProgress, TResult> listener) {
		onExecuteListeners.add(listener);
	}


	@Override
	public void addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		onProgressListeners.add(listener);
	}
	
	@Override
	public void addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		onCompleteListeners.add(listener);
	}

	@Override
	public void addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		onErrorListeners.add(listener);
	}

	@Override
	public TResult getResult() throws ExecutionException, InterruptedException {
		return this.get();
	}

	@Override
	public void setResult(TResult result) {
		mResult = result;		
	}

	@Override
	public void removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		onStartListeners.remove(listener);
	}

	@Override
	public void removeOnExecuteListener(OnExecuteListener<TParams, TProgress, TResult> listener) {
		onExecuteListeners.remove(listener);		
	}

	@Override
	public void removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		onCompleteListeners.remove(listener);		
	}

	@Override
	public void removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		onErrorListeners.remove(listener);		
	}


	@Override
	public void removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		onProgressListeners.remove(listener);		
	}

	@Override
	public SimpleTaskState getState() {
		return mState;
	}
}
