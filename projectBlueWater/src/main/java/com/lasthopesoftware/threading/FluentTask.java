package com.lasthopesoftware.threading;

import android.os.AsyncTask;

import com.lasthopesoftware.callables.IOneParameterCallable;
import com.lasthopesoftware.callables.ITwoParameterCallable;
import com.lasthopesoftware.runnables.IOneParameterRunnable;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public abstract class FluentTask<TParams, TProgress, TResult>  {

	private volatile SimpleTaskState state = SimpleTaskState.INITIALIZED;

//	private final OnExecuteListener<TParams, TProgress, TResult> onExecuteListener;
	private ITwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> twoParameterOnCompleteListener;
	private IOneParameterRunnable<TResult> oneParameterOnCompleteListener;

	private IOneParameterCallable<Exception, Boolean> oneParameterOnErrorListener;
	private ITwoParameterCallable<FluentTask<TParams, TProgress, TResult>, Exception, Boolean> twoParameterOnErrorListener;

	private final AsyncExceptionTask<TParams, TProgress, TResult> task = new AsyncExceptionTask<TParams, TProgress, TResult>() {

				@SafeVarargs
				@Override
				protected final TResult doInBackground(TParams... params) {
					return doInBackground(params);
				}

				@Override
				protected final void onPostExecute(TResult result, Exception exception) {
					if (handleError(exception)) return;

					state = SimpleTaskState.SUCCESS;

					if (twoParameterOnCompleteListener != null)
						twoParameterOnCompleteListener.run(FluentTask.this, result);

					if (oneParameterOnCompleteListener != null)
						oneParameterOnCompleteListener.run(result);
				}

				@Override
				protected final void onCancelled(TResult result, Exception exception) {
					if (handleError(exception)) return;

					state = SimpleTaskState.CANCELLED;
					super.onCancelled(result, exception);
				}
			};


//	@SafeVarargs
//	public static <TParams, TProgress, TResult> FluentTask<TParams, TProgress, TResult> executeNew(OnExecuteListener<TParams, TProgress, TResult> onExecuteListener, TParams... params) {
//		final FluentTask<TParams, TProgress, TResult> newSimpleTask = new FluentTask<>(onExecuteListener);
//		newSimpleTask.execute(params);
//		return newSimpleTask;
//	}
//
//	@SafeVarargs
//	public static <TParams, TProgress, TResult> FluentTask<TParams, TProgress, TResult> executeNew(Executor executor, OnExecuteListener<TParams, TProgress, TResult> onExecuteListener, TParams... params) {
//		final FluentTask<TParams, TProgress, TResult> newSimpleTask = new FluentTask<>(onExecuteListener);
//		newSimpleTask.execute(executor, params);
//		return newSimpleTask;
//	}
	
//	public FluentTask(OnExecuteListener<TParams, TProgress, TResult> onExecuteListener) {
//		this.onExecuteListener = onExecuteListener;
//	}

	public FluentTask() {
		super();
	}


	public FluentTask<TParams, TProgress, TResult> execute(TParams... params) {
		return execute(AsyncTask.SERIAL_EXECUTOR, params);
	}

	public FluentTask<TParams, TProgress, TResult> execute(Executor exec, TParams... params) {
		state = SimpleTaskState.EXECUTING;
		task.executeOnExecutor(exec, params);
		return this;
	}
//
//	@SafeVarargs
//	private final TResult executeListener(TParams... params) {
//		try {
//			result = onExecuteListener.onExecute(this, params);
//			state = SimpleTaskState.SUCCESS;
//		} catch (Exception exception) {
//			this.exception = exception;
//			state = SimpleTaskState.ERROR;
//		}
//		return result;
//	}

	/**
	 * 
	 * @return True if there is an error and it is handled
	 */
	private boolean handleError(Exception exception) {
		return exception != null &&
					(twoParameterOnErrorListener != null && twoParameterOnErrorListener.call(this, exception)) |
					(oneParameterOnErrorListener != null && oneParameterOnErrorListener.call(exception));

	}

	public TResult get() throws ExecutionException, InterruptedException {
		return task.get();
	}

	public FluentTask<TParams, TProgress, TResult> cancel() {
		return cancel(true);
	}

	public FluentTask<TParams, TProgress, TResult> cancel(boolean interrupt) {
		task.cancel(interrupt);
		return this;
	}

	public boolean isCancelled() {
		return task.isCancelled();
	}
//
//	public SimpleTaskState getState() {
//		return state;
//	}

//	@Override
//	protected final void onPostExecute(TResult tResult, Exception exception) {
//		if (handleError()) return;
//
//		super.onPostExecute(result);
//
//		if (twoParameterOnCompleteListener != null)
//			twoParameterOnCompleteListener.run(FluentTask.this, result);
//
//		if (oneParameterOnCompleteListener != null)
//			oneParameterOnCompleteListener.run(result);
//	}
//
//	@Override
//	protected final void onCancelled(TResult result, Exception exception) {
//		if (handleError()) return;
//
//		state = SimpleTaskState.CANCELLED;
//		super.onCancelled(result, exception);
//	}

	protected abstract TResult doInBackground(TParams... params);

	protected void setException(Exception exception) {
		task.setException(exception);
	}

	public FluentTask<TParams, TProgress, TResult> onComplete(ITwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> listener) {
		twoParameterOnCompleteListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onComplete(IOneParameterRunnable<TResult> listener) {
		oneParameterOnCompleteListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onError(ITwoParameterCallable<FluentTask<TParams, TProgress, TResult>, Exception, Boolean> listener) {
		twoParameterOnErrorListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onError(IOneParameterCallable<Exception, Boolean> listener) {
		oneParameterOnErrorListener = listener;
		return this;
	}
}
