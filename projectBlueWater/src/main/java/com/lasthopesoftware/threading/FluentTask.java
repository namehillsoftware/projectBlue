package com.lasthopesoftware.threading;

import android.os.AsyncTask;

import com.lasthopesoftware.callables.IThreeParameterCallable;
import com.lasthopesoftware.callables.ITwoParameterCallable;
import com.lasthopesoftware.runnables.IOneParameterRunnable;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class FluentTask<TParams, TProgress, TResult> {

	private volatile boolean isErrorHandled = false;

	private volatile SimpleTaskState state = SimpleTaskState.INITIALIZED;

	private final OnExecuteListener<TParams, TProgress, TResult> onExecuteListener;
	private ITwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> twoParameterOnCompleteListener;
	private IOneParameterRunnable<TResult> oneParameterOnCompleteListener;
	private IThreeParameterCallable<FluentTask<TParams, TProgress, TResult>, Boolean, Exception, Boolean> threeParameterOnErrorListener;
	private ITwoParameterCallable<Boolean, Exception, Boolean> twoParameterOnErrorListener;

	private TResult result;
	private Exception exception;

	private final Lazy<AsyncTask<TParams, TProgress, TResult>> task = new Lazy<>(new Callable<AsyncTask<TParams, TProgress, TResult>>() {
		@Override
		public AsyncTask<TParams, TProgress, TResult> call() throws Exception {
			return new AsyncTask<TParams, TProgress, TResult>() {

				@SafeVarargs
				@Override
				protected final TResult doInBackground(TParams... params) {
					return executeListener(params);
				}

				@Override
				protected final void onPostExecute(TResult result) {
					if (handleError()) return;

					super.onPostExecute(result);

					if (twoParameterOnCompleteListener != null)
						twoParameterOnCompleteListener.run(FluentTask.this, result);

					if (oneParameterOnCompleteListener != null)
						oneParameterOnCompleteListener.run(result);
				}

				@Override
				protected final void onCancelled(TResult result) {
					if (handleError()) return;

					state = SimpleTaskState.CANCELLED;
					super.onCancelled(result);
				}
			};
		}
	});


	@SafeVarargs
	public static <TParams, TProgress, TResult> FluentTask<TParams, TProgress, TResult> executeNew(OnExecuteListener<TParams, TProgress, TResult> onExecuteListener, TParams... params) {
		final FluentTask<TParams, TProgress, TResult> newSimpleTask = new FluentTask<>(onExecuteListener);
		newSimpleTask.execute(params);
		return newSimpleTask;
	}
	
	@SafeVarargs
	public static <TParams, TProgress, TResult> FluentTask<TParams, TProgress, TResult> executeNew(Executor executor, OnExecuteListener<TParams, TProgress, TResult> onExecuteListener, TParams... params) {
		final FluentTask<TParams, TProgress, TResult> newSimpleTask = new FluentTask<>(onExecuteListener);
		newSimpleTask.execute(executor, params);
		return newSimpleTask;
	}
	
	public FluentTask(OnExecuteListener<TParams, TProgress, TResult> onExecuteListener) {
		this.onExecuteListener = onExecuteListener;
	}
		
	@SafeVarargs
	public final FluentTask<TParams, TProgress, TResult> execute(TParams... params) {
		return execute(AsyncTask.SERIAL_EXECUTOR, params);
	}
	
	@SuppressWarnings("unchecked")
	public final FluentTask<TParams, TProgress, TResult> execute(Executor exec, TParams... params) {
		state = SimpleTaskState.EXECUTING;
		task.getObject().executeOnExecutor(exec, params);
		return this;
	}
		
	@SafeVarargs
	private final TResult executeListener(TParams... params) {	
		try {
			result = onExecuteListener.onExecute(this, params);
			state = SimpleTaskState.SUCCESS;
		} catch (Exception exception) {
			this.exception = exception;
			state = SimpleTaskState.ERROR;
		}
		return result;
	}

	public Exception getException() {
		return exception;
	}
	
	/**
	 * 
	 * @return True if there is an error and it is handled
	 */
	private boolean handleError() {
		return state == SimpleTaskState.ERROR &&
				(isErrorHandled ||
						(threeParameterOnErrorListener != null && threeParameterOnErrorListener.call(this, isErrorHandled, exception)) ||
						(twoParameterOnErrorListener != null && twoParameterOnErrorListener.call(isErrorHandled, exception)));

	}

	public TResult get() throws ExecutionException, InterruptedException {
		if (state != SimpleTaskState.EXECUTING) execute();
		final TResult result = task.getObject().get();
		
		if (exception != null) throw new ExecutionException(exception);
		
		return result; 
	}

	public FluentTask<TParams, TProgress, TResult> cancel(boolean interrupt) {
		task.getObject().cancel(interrupt);
		return this;
	}

	public boolean isCancelled() {
		return state == SimpleTaskState.CANCELLED;
	}

	public SimpleTaskState getState() {
		return state;
	}

	public FluentTask<TParams, TProgress, TResult> onComplete(ITwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> listener) {
		if (state == SimpleTaskState.SUCCESS) listener.run(this, result);

		twoParameterOnCompleteListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onComplete(IOneParameterRunnable<TResult> listener) {
		if (state == SimpleTaskState.SUCCESS) listener.run(result);

		oneParameterOnCompleteListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onError(IThreeParameterCallable<FluentTask<TParams, TProgress, TResult>, Boolean, Exception, Boolean> listener) {
		if (state == SimpleTaskState.ERROR) listener.call(this, isErrorHandled, exception);

		threeParameterOnErrorListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onError(ITwoParameterCallable<Boolean, Exception, Boolean> listener) {
		if (state == SimpleTaskState.ERROR) listener.call(isErrorHandled, exception);

		twoParameterOnErrorListener = listener;
		return this;
	}
}
