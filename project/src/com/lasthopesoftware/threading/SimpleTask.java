package com.lasthopesoftware.threading;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import android.os.AsyncTask;

public class SimpleTask<TParams, TProgress, TResult> implements ISimpleTask<TParams, TProgress, TResult>, Runnable {

	private final AsyncTask<TParams, TProgress, TResult> mTask;
	
	private TResult mResult;
	private volatile SimpleTaskState mState = SimpleTaskState.INITIALIZED;
	
	private final OnExecuteListener<TParams, TProgress, TResult> mOnExecuteListener;
	private ConcurrentLinkedQueue<OnProgressListener<TParams, TProgress, TResult>> mOnProgressListeners = null;
	private ConcurrentLinkedQueue<OnCompleteListener<TParams, TProgress, TResult>> mOnCompleteListeners = null;
	private ConcurrentLinkedQueue<OnCancelListener<TParams, TProgress, TResult>> mOnCancelListeners = null;
	private ConcurrentLinkedQueue<OnStartListener<TParams, TProgress, TResult>> mOnStartListeners = null;
	private ConcurrentLinkedQueue<OnErrorListener<TParams, TProgress, TResult>> mOnErrorListeners = null;
	private Exception mException;
		
	public SimpleTask(OnExecuteListener<TParams, TProgress, TResult> onExecuteListener) {
		mOnExecuteListener = onExecuteListener;
		
		final SimpleTask<TParams, TProgress, TResult> _this = this;
		mTask = new AsyncTask<TParams, TProgress, TResult>() {

			@Override
			protected final void onPreExecute() {
				super.onPreExecute();
				if (mOnStartListeners == null) return;
				for (OnStartListener<TParams, TProgress, TResult> listener : mOnStartListeners) listener.onStart(_this);
			}
			
			@Override
			@SuppressWarnings("unchecked")
			protected final TResult doInBackground(TParams... params) {
				return executeListener(params);
			}
			
			@Override
			@SuppressWarnings("unchecked")
			protected final void onProgressUpdate(TProgress... values) {
				if (mOnProgressListeners == null) return;
				for (OnProgressListener<TParams, TProgress, TResult> progressListener : mOnProgressListeners) progressListener.onReportProgress(_this, values);
			}
			
			@Override
			protected final void onPostExecute(TResult result) {

				if (isErrorHandled()) return;
				
				super.onPostExecute(result);
				
				if (mOnCompleteListeners == null) return;
				for (OnCompleteListener<TParams, TProgress, TResult> completeListener : mOnCompleteListeners) completeListener.onComplete(_this, result);
			}
				
			@Override
			protected final void onCancelled(TResult result) {
				if (isErrorHandled()) return;
				
				mState = SimpleTaskState.CANCELLED;
				super.onCancelled(result);
				if (mOnCancelListeners == null) return;
				for (OnCancelListener<TParams, TProgress, TResult> cancelListener : mOnCancelListeners) cancelListener.onCancel(_this, result);
			}
			
		};
	}
		
	@SuppressWarnings("unchecked")
	public final SimpleTask<TParams, TProgress, TResult> execute(TParams... params) {
		mTask.execute(params);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public final SimpleTask<TParams, TProgress, TResult> executeOnExecutor(Executor exec, TParams... params) {
		mTask.executeOnExecutor(exec, params);
		return this;
	}
	
	@Override
	public void run() {
		executeListener();
	}
	
	@SafeVarargs
	private final TResult executeListener(TParams... params) {
		mState = SimpleTaskState.EXECUTING;
		
		try {
			mResult = mOnExecuteListener.onExecute(this, params);
			mState = SimpleTaskState.SUCCESS;
		} catch (Exception exception) {
			mException = exception;
			mState = SimpleTaskState.ERROR;
		}
		return mResult;
	}
	
	@Override
	public Exception getException() {
		return mException;
	}
	
	/**
	 * 
	 * @return True if there is an error and it is handled
	 */
	private final boolean isErrorHandled() {
		if (mState != SimpleTaskState.ERROR) return false;
		if (mOnErrorListeners != null) {
			boolean isHandled = false;
			for (OnErrorListener<TParams, TProgress, TResult> errorListener : mOnErrorListeners)
				isHandled |= errorListener.onError(this, isHandled, mException);
			return isHandled;
		}
		return false;
	}
	
	@Override
	public TResult get() throws Exception {
		final TResult result = mTask.get();
		
		if (mException != null) throw mException;
		
		return result; 
	}

	@Override
	public void cancel(boolean interrupt) {
		mTask.cancel(interrupt);
	}
	
	@Override
	public boolean isCancelled() {
		return mState == SimpleTaskState.CANCELLED;
	}

	@Override
	public SimpleTaskState getState() {
		return mState;
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
}
