package com.lasthopesoftware.threading;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import android.os.AsyncTask;

public class SimpleTask<TParams, TProgress, TResult> implements ISimpleTask<TParams, TProgress, TResult> {

	private AsyncTask<TParams, TProgress, TResult> mTask;
	
	private TResult mResult;
	private volatile SimpleTaskState mState = SimpleTaskState.INITIALIZED;
	
	private final OnExecuteListener<TParams, TProgress, TResult> mOnExecuteListener;
	private ConcurrentLinkedQueue<OnProgressListener<TParams, TProgress, TResult>> mOnProgressListeners = null;
	private ConcurrentLinkedQueue<OnCompleteListener<TParams, TProgress, TResult>> mOnCompleteListeners = null;
	private ConcurrentLinkedQueue<OnCancelListener<TParams, TProgress, TResult>> mOnCancelListeners = null;
	private ConcurrentLinkedQueue<OnStartListener<TParams, TProgress, TResult>> mOnStartListeners = null;
	private ConcurrentLinkedQueue<OnErrorListener<TParams, TProgress, TResult>> mOnErrorListeners = null;
	private Exception mException;
	
	private final Object syncObj = new Object();
	
	@SafeVarargs
	public final static <TParams, TProgress, TResult> SimpleTask<TParams, TProgress, TResult> startNew(OnExecuteListener<TParams, TProgress, TResult> onExecuteListener, TParams... params) {
		final SimpleTask<TParams, TProgress, TResult> newSimpleTask = new SimpleTask<TParams, TProgress, TResult>(onExecuteListener);
		newSimpleTask.execute(params);
		return newSimpleTask;
	}
	
	@SafeVarargs
	public final static <TParams, TProgress, TResult> SimpleTask<TParams, TProgress, TResult> startNew(Executor executor, OnExecuteListener<TParams, TProgress, TResult> onExecuteListener, TParams... params) {
		final SimpleTask<TParams, TProgress, TResult> newSimpleTask = new SimpleTask<TParams, TProgress, TResult>(onExecuteListener);
		newSimpleTask.execute(executor, params);
		return newSimpleTask;
	}
	
	public SimpleTask(OnExecuteListener<TParams, TProgress, TResult> onExecuteListener) {
		mOnExecuteListener = onExecuteListener;
	}
	
	private final AsyncTask<TParams, TProgress, TResult> getTask() {
		if (mTask != null) return mTask;
		
		synchronized(syncObj) {
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
		
		return mTask;
	}
		
	@SafeVarargs
	public final SimpleTask<TParams, TProgress, TResult> execute(TParams... params) {
		return execute(AsyncTask.SERIAL_EXECUTOR, params);
	}
	
	@SuppressWarnings("unchecked")
	public final SimpleTask<TParams, TProgress, TResult> execute(Executor exec, TParams... params) {
		mState = SimpleTaskState.EXECUTING;
		getTask().executeOnExecutor(exec, params);
		return this;
	}
		
	@SafeVarargs
	private final TResult executeListener(TParams... params) {	
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
		if (mState != SimpleTaskState.EXECUTING) execute();
		final TResult result = getTask().get();
		
		if (mException != null) throw mException;
		
		return result; 
	}

	@Override
	public ISimpleTask<TParams, TProgress, TResult> cancel(boolean interrupt) {
		getTask().cancel(interrupt);
		return this;
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
	public ISimpleTask<TParams, TProgress, TResult> addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		mOnStartListeners = addListener(listener, mOnStartListeners);
		return this;
	}

	@Override
	public ISimpleTask<TParams, TProgress, TResult> addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		mOnProgressListeners = addListener(listener, mOnProgressListeners);
		return this;
	}
	
	@Override
	public ISimpleTask<TParams, TProgress, TResult> addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		if (mState == SimpleTaskState.SUCCESS) listener.onComplete(this, mResult);

		mOnCompleteListeners = addListener(listener, mOnCompleteListeners);
		return this;
	}
	
	@Override
	public ISimpleTask<TParams, TProgress, TResult> addOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<TParams, TProgress, TResult> listener) {
		mOnCancelListeners = addListener(listener, mOnCancelListeners);
		return this;
	}

	@Override
	public ISimpleTask<TParams, TProgress, TResult> addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		mOnErrorListeners = addListener(listener, mOnErrorListeners);
		return this;
	}

	@Override
	public ISimpleTask<TParams, TProgress, TResult> removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnStartListeners);
		return this;
	}

	@Override
	public ISimpleTask<TParams, TProgress, TResult> removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnCompleteListeners);
		return this;
	}

	@Override
	public ISimpleTask<TParams, TProgress, TResult> removeOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnCancelListeners);
		return this;
	}
	
	@Override
	public ISimpleTask<TParams, TProgress, TResult> removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnErrorListeners);
		return this;
	}

	@Override
	public ISimpleTask<TParams, TProgress, TResult> removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener) {
		removeListener(listener, mOnProgressListeners);
		return this;
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
