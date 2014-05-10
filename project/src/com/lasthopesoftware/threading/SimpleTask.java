package com.lasthopesoftware.threading;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;

public class SimpleTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> implements ISimpleTask<TParams, TProgress, TResult> {

	private TResult mResult;
	private SimpleTaskState mState = SimpleTaskState.INITIALIZED;
	
	OnExecuteListener<TParams, TProgress, TResult> onExecuteListener = null;
	ConcurrentLinkedQueue<OnProgressListener<TParams, TProgress, TResult>> onProgressListeners = new ConcurrentLinkedQueue<OnProgressListener<TParams, TProgress, TResult>>();
	ConcurrentLinkedQueue<OnCompleteListener<TParams, TProgress, TResult>> onCompleteListeners = new ConcurrentLinkedQueue<OnCompleteListener<TParams, TProgress, TResult>>();
	ConcurrentLinkedQueue<OnStartListener<TParams, TProgress, TResult>> onStartListeners = new ConcurrentLinkedQueue<OnStartListener<TParams, TProgress, TResult>>();
	ConcurrentLinkedQueue<OnErrorListener<TParams, TProgress, TResult>> onErrorListeners = new ConcurrentLinkedQueue<OnErrorListener<TParams, TProgress, TResult>>();
	LinkedList<Exception> exceptions = new LinkedList<Exception>();
		
	@Override
	protected void onPreExecute() {
		for (OnStartListener<TParams, TProgress, TResult> listener : onStartListeners) listener.onStart(this);
	}
	
	@Override
	protected TResult doInBackground(TParams... params) {
		mState = SimpleTaskState.EXECUTING;
		
		try {
			mResult = onExecuteListener.onExecute(this, params);
			mState = SimpleTaskState.SUCCESS;
		} catch (Exception ex) {
			exceptions.add(ex);
			mState = SimpleTaskState.ERROR;
			
			for (OnErrorListener<TParams, TProgress, TResult> errorListener : onErrorListeners) errorListener.onError(this, ex);
		}
		return mResult;
	}
	
	@Override
	public LinkedList<Exception> getExceptions() {
		return exceptions;
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
	protected void onCancelled(TResult result) {
		mState = SimpleTaskState.CANCELLED;
	}
	
	@Override
	public void addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		if (listener != null)
			onStartListeners.add(listener);
	}
	
	@Override
	public void setOnExecuteListener(OnExecuteListener<TParams, TProgress, TResult> listener) {
		onExecuteListener = listener;
	}


	@Override
	public void addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		if (listener != null)
			onProgressListeners.add(listener);
	}
	
	@Override
	public void addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		if (listener != null)
			onCompleteListeners.add(listener);
	}

	@Override
	public void addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		if (listener != null)
			onErrorListeners.add(listener);
	}

	@Override
	public TResult getResult() throws ExecutionException, InterruptedException {
		return this.get();
	}

	@Override
	public void removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		if (onStartListeners.contains(listener)) onStartListeners.remove(listener);
	}

	@Override
	public void removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		if (onCompleteListeners.contains(listener)) onCompleteListeners.remove(listener);		
	}

	@Override
	public void removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		if (onErrorListeners.contains(listener)) onErrorListeners.remove(listener);		
	}


	@Override
	public void removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		if (onProgressListeners.contains(listener)) onProgressListeners.remove(listener);		
	}

	@Override
	public SimpleTaskState getState() {
		return mState;
	}
}
