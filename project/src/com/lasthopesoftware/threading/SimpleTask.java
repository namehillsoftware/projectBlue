package com.lasthopesoftware.threading;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;

public class SimpleTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> implements ISimpleTask<TParams, TProgress, TResult> {

	private TResult mResult;
	private volatile SimpleTaskState mState = SimpleTaskState.INITIALIZED;
	
	OnExecuteListener<TParams, TProgress, TResult> onExecuteListener = null;
	ConcurrentLinkedQueue<OnProgressListener<TParams, TProgress, TResult>> onProgressListeners = null;
	ConcurrentLinkedQueue<OnCompleteListener<TParams, TProgress, TResult>> onCompleteListeners = null;
	ConcurrentLinkedQueue<OnStartListener<TParams, TProgress, TResult>> onStartListeners = null;
	ConcurrentLinkedQueue<OnErrorListener<TParams, TProgress, TResult>> onErrorListeners = null;
	LinkedList<Exception> exceptions = new LinkedList<Exception>();
		
	@Override
	protected void onPreExecute() {
		if (onStartListeners == null) return;
		for (OnStartListener<TParams, TProgress, TResult> listener : onStartListeners) listener.onStart(this);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected TResult doInBackground(TParams... params) {
		exceptions.clear();
		mState = SimpleTaskState.EXECUTING;
		
		try {
			mResult = onExecuteListener.onExecute(this, params);
			mState = SimpleTaskState.SUCCESS;
		} catch (Exception ex) {
			exceptions.add(ex);
			mState = SimpleTaskState.ERROR;
			if (onErrorListeners != null)
				for (OnErrorListener<TParams, TProgress, TResult> errorListener : onErrorListeners) errorListener.onError(this, ex);
		}
		return mResult;
	}
	
	@Override
	public LinkedList<Exception> getExceptions() {
		return exceptions;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void reportProgress(TProgress... values) {
		this.publishProgress(values);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void onProgressUpdate(TProgress... values) {
		if (onProgressListeners == null) return;
		for (OnProgressListener<TParams, TProgress, TResult> progressListener : onProgressListeners) progressListener.onReportProgress(this, values);
	}
	
	@Override
	protected void onPostExecute(TResult result) {
		if (onCompleteListeners == null) return;
		for (OnCompleteListener<TParams, TProgress, TResult> completeListener : onCompleteListeners) completeListener.onComplete(this, result);
	}
	
	@Override
	protected void onCancelled(TResult result) {
		mState = SimpleTaskState.CANCELLED;
	}
	
	@Override
	public void addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		if (listener == null) return;
		if (onStartListeners == null) onStartListeners = new ConcurrentLinkedQueue<ISimpleTask.OnStartListener<TParams,TProgress,TResult>>(); 
		onStartListeners.add(listener);
	}
	
	@Override
	public void setOnExecuteListener(OnExecuteListener<TParams, TProgress, TResult> listener) {
		onExecuteListener = listener;
	}


	@Override
	public void addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		if (listener == null) return;
		if (onProgressListeners == null) onProgressListeners = new ConcurrentLinkedQueue<ISimpleTask.OnProgressListener<TParams,TProgress,TResult>>();
		onProgressListeners.add(listener);
	}
	
	@Override
	public void addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		if (listener == null) return;
		if (onCompleteListeners == null) onCompleteListeners = new ConcurrentLinkedQueue<ISimpleTask.OnCompleteListener<TParams,TProgress,TResult>>();
		onCompleteListeners.add(listener);
	}

	@Override
	public void addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		if (listener == null) return;
		if (onErrorListeners == null) onErrorListeners = new ConcurrentLinkedQueue<ISimpleTask.OnErrorListener<TParams,TProgress,TResult>>();
		onErrorListeners.add(listener);
	}

	@Override
	public TResult getResult() throws ExecutionException, InterruptedException {
		return this.get();
	}

	@Override
	public void removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		if (onStartListeners == null || !onStartListeners.contains(listener)) return;
		onStartListeners.remove(listener);
	}

	@Override
	public void removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		if (onCompleteListeners == null || !onCompleteListeners.contains(listener)) return;
		onCompleteListeners.remove(listener);		
	}

	@Override
	public void removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		if (onErrorListeners == null || !onErrorListeners.contains(listener)) return;
		onErrorListeners.remove(listener);		
	}


	@Override
	public void removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		if (onProgressListeners == null || !onProgressListeners.contains(listener)) return;
		onProgressListeners.remove(listener);		
	}

	@Override
	public SimpleTaskState getState() {
		return mState;
	}
}
