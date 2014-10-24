package com.lasthopesoftware.threading;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;

public class SimpleTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> implements ISimpleTask<TParams, TProgress, TResult> {

	private TResult mResult;
	private volatile SimpleTaskState mState = SimpleTaskState.INITIALIZED;
	
	private OnExecuteListener<TParams, TProgress, TResult> mOnExecuteListener = null;
	private ConcurrentLinkedQueue<OnProgressListener<TParams, TProgress, TResult>> mOnProgressListeners = null;
	private ConcurrentLinkedQueue<OnCompleteListener<TParams, TProgress, TResult>> mOnCompleteListeners = null;
	private ConcurrentLinkedQueue<OnCancelListener<TParams, TProgress, TResult>> mOnCancelListeners = null;
	private ConcurrentLinkedQueue<OnStartListener<TParams, TProgress, TResult>> mOnStartListeners = null;
	private ConcurrentLinkedQueue<OnErrorListener<TParams, TProgress, TResult>> mOnErrorListeners = null;
	private LinkedList<Exception> exceptions = new LinkedList<Exception>();
	
	public SimpleTask() {
		super();
	}
	
	public SimpleTask(OnExecuteListener<TParams, TProgress, TResult> onExecuteListener) {
		this();
		setOnExecuteListener(onExecuteListener);
	}
	
	@Override
	protected final void onPreExecute() {
		super.onPreExecute();
		if (mOnStartListeners == null) return;
		for (OnStartListener<TParams, TProgress, TResult> listener : mOnStartListeners) listener.onStart(this);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected final TResult doInBackground(TParams... params) {
		mState = SimpleTaskState.EXECUTING;
		exceptions.clear();
		
		try {
			mResult = mOnExecuteListener.onExecute(this, params);
			mState = SimpleTaskState.SUCCESS;
		} catch (Exception ex) {
			exceptions.add(ex);
			mState = SimpleTaskState.ERROR;
			if (mOnErrorListeners != null)
				for (OnErrorListener<TParams, TProgress, TResult> errorListener : mOnErrorListeners) errorListener.onError(this, ex);
		}
		return mResult;
	}
	
	@Override
	public LinkedList<Exception> getExceptions() {
		return exceptions;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final void reportProgress(TProgress... values) {
		this.publishProgress(values);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected final void onProgressUpdate(TProgress... values) {
		if (mOnProgressListeners == null) return;
		for (OnProgressListener<TParams, TProgress, TResult> progressListener : mOnProgressListeners) progressListener.onReportProgress(this, values);
	}
	
	@Override
	protected final void onPostExecute(TResult result) {
		super.onPostExecute(result);
		if (mOnCompleteListeners == null) return;
		for (OnCompleteListener<TParams, TProgress, TResult> completeListener : mOnCompleteListeners) completeListener.onComplete(this, result);
	}
	
	@Override
	protected final void onCancelled(TResult result) {
		mState = SimpleTaskState.CANCELLED;
		super.onCancelled(result);
		if (mOnCancelListeners == null) return;
		for (OnCancelListener<TParams, TProgress, TResult> cancelListener : mOnCancelListeners) cancelListener.onCancel(this, result);
	}
	
	@Override
	public void setOnExecuteListener(OnExecuteListener<TParams, TProgress, TResult> listener) {
		mOnExecuteListener = listener;
	}

	@Override
	public TResult getResult() throws ExecutionException, InterruptedException {
		return this.get();
	}
	
	@Override
	public void addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		mOnStartListeners = addListener(listener, mOnStartListeners);
	}

	@Override
	public void addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		mOnProgressListeners = addListener(listener, mOnProgressListeners);
	}
	
	@Override
	public void addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		if (mState == SimpleTaskState.SUCCESS) listener.onComplete(this, mResult);

		mOnCompleteListeners = addListener(listener, mOnCompleteListeners);
	}
	
	@Override
	public void addOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<TParams, TProgress, TResult> listener) {
		mOnCancelListeners = addListener(listener, mOnCancelListeners);
	}

	@Override
	public void addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		mOnErrorListeners = addListener(listener, mOnErrorListeners);
	}

	@Override
	public void removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnStartListeners);
	}

	@Override
	public void removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnCompleteListeners);
	}

	@Override
	public void removeOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnCancelListeners);
	}
	
	@Override
	public void removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnErrorListeners);
	}

	@Override
	public void removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnProgressListeners);
	}
	
	private static final <T> ConcurrentLinkedQueue<T> addListener(T listener, ConcurrentLinkedQueue<T> listenerQueue) {
		if (listener == null) return listenerQueue;
		if (listenerQueue == null) listenerQueue = new ConcurrentLinkedQueue<T>();
		listenerQueue.add(listener);
		return listenerQueue;
	}
	
	private static final <T> void removeListener(T listener, ConcurrentLinkedQueue<T> listenerQueue) {
		if (listenerQueue == null || !listenerQueue.contains(listener)) return;
		listenerQueue.remove(listener);
	}

	@Override
	public synchronized SimpleTaskState getState() {
		return mState;
	}
}
