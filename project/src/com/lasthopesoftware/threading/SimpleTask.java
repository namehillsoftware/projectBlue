package com.lasthopesoftware.threading;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;

public class SimpleTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> implements ISimpleTask<TParams, TProgress, TResult> {

	private TResult mResult;
	private volatile SimpleTaskState mState = SimpleTaskState.INITIALIZED;
	
	private OnExecuteListener<TParams, TProgress, TResult> onExecuteListener = null;
	private ConcurrentLinkedQueue<OnProgressListener<TParams, TProgress, TResult>> onProgressListeners = null;
	private ConcurrentLinkedQueue<OnCompleteListener<TParams, TProgress, TResult>> onCompleteListeners = null;
	private ConcurrentLinkedQueue<OnCancelListener<TParams, TProgress, TResult>> onCancelListeners = null;
	private ConcurrentLinkedQueue<OnStartListener<TParams, TProgress, TResult>> onStartListeners = null;
	private ConcurrentLinkedQueue<OnErrorListener<TParams, TProgress, TResult>> onErrorListeners = null;
	private LinkedList<Exception> exceptions = new LinkedList<Exception>();
		
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (onStartListeners == null) return;
		for (OnStartListener<TParams, TProgress, TResult> listener : onStartListeners) listener.onStart(this);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected TResult doInBackground(TParams... params) {
		mState = SimpleTaskState.EXECUTING;
		exceptions.clear();
		
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
		super.onPostExecute(result);
		if (onCompleteListeners == null) return;
		for (OnCompleteListener<TParams, TProgress, TResult> completeListener : onCompleteListeners) completeListener.onComplete(this, result);
	}
	
	@Override
	protected void onCancelled(TResult result) {
		mState = SimpleTaskState.CANCELLED;
		super.onCancelled(result);
		if (onCancelListeners == null) return;
		for (OnCancelListener<TParams, TProgress, TResult> cancelListener : onCancelListeners) cancelListener.onCancel(this, result);
	}
	
	@Override
	public void setOnExecuteListener(OnExecuteListener<TParams, TProgress, TResult> listener) {
		onExecuteListener = listener;
	}

	@Override
	public TResult getResult() throws ExecutionException, InterruptedException {
		return this.get();
	}
	
	@Override
	public void addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		onStartListeners = addListener(listener, onStartListeners);
	}

	@Override
	public void addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		onProgressListeners = addListener(listener, onProgressListeners);
	}
	
	@Override
	public void addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		onCompleteListeners = addListener(listener, onCompleteListeners);
	}
	
	@Override
	public void addOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<TParams, TProgress, TResult> listener) {
		onCancelListeners = addListener(listener, onCancelListeners);
	}

	@Override
	public void addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		onErrorListeners = addListener(listener, onErrorListeners);
	}

	@Override
	public void removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, onStartListeners);
	}

	@Override
	public void removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, onCompleteListeners);
	}

	@Override
	public void removeOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, onCancelListeners);
	}
	
	@Override
	public void removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, onErrorListeners);
	}

	@Override
	public void removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, onProgressListeners);
	}
	
	private <T> ConcurrentLinkedQueue<T> addListener(T listener, ConcurrentLinkedQueue<T> listenerQueue) {
		if (listener == null) return listenerQueue;
		if (listenerQueue == null) listenerQueue = new ConcurrentLinkedQueue<T>();
		listenerQueue.add(listener);
		return listenerQueue;
	}
	
	private <T> void removeListener(T listener, ConcurrentLinkedQueue<T> listenerQueue) {
		if (listenerQueue == null || !listenerQueue.contains(listener)) return;
		listenerQueue.remove(listener);
	}

	@Override
	public synchronized SimpleTaskState getState() {
		return mState;
	}
}
