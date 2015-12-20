package com.lasthopesoftware.threading;

import android.os.AsyncTask;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.callables.TwoParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public abstract class FluentTask<TParams, TProgress, TResult>  {

	private final TParams[] params;
	private final Executor defaultExecutor;

	private TwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> twoParameterOnCompleteListener;
	private OneParameterRunnable<TResult> oneParameterOnCompleteListener;

	private OneParameterCallable<Exception, Boolean> oneParameterOnErrorListener;
	private TwoParameterCallable<FluentTask<TParams, TProgress, TResult>, Exception, Boolean> twoParameterOnErrorListener;

	private final Lazy<AsyncExceptionTask<Void, TProgress, TResult>> task = new Lazy<>(new Callable<AsyncExceptionTask<Void, TProgress, TResult>>() {

		@Override
		public AsyncExceptionTask<Void, TProgress, TResult> call() throws Exception {
			return new AsyncExceptionTask<Void, TProgress, TResult>(){

				@Override
				protected final TResult doInBackground(Void... params) {
					return executeInBackground(FluentTask.this.params);
				}

				@Override
				protected final void onPostExecute(TResult result, Exception exception) {
					if (handleError(exception)) return;

					if (twoParameterOnCompleteListener != null)
						twoParameterOnCompleteListener.run(FluentTask.this, result);

					if (oneParameterOnCompleteListener != null)
						oneParameterOnCompleteListener.run(result);
				}

				@Override
				protected final void onCancelled(TResult result, Exception exception) {
					if (handleError(exception)) return;

					super.onCancelled(result, exception);
				}
			};
		}
	});

	@SafeVarargs
	public FluentTask(TParams... params) {
		this(AsyncTask.SERIAL_EXECUTOR, params);
	}

	@SafeVarargs
	public FluentTask(Executor defaultExecutor, TParams... params) {
		this.params = params;
		this.defaultExecutor = defaultExecutor;
	}

	public void execute() {
		executeTask();
	}

	public void execute(Executor exec) {
		executeTask(exec);
	}

	public TResult get() throws ExecutionException, InterruptedException {
		return executeTask().get();
	}

	public TResult get(Executor executor) throws ExecutionException, InterruptedException {
		return executeTask(executor).get();
	}

	private AsyncTask<Void, TProgress, TResult> executeTask() {
		return executeTask(defaultExecutor);
	}

	private AsyncTask<Void, TProgress, TResult> executeTask(Executor exec) {
		return task.getObject().executeOnExecutor(exec);
	}

	/**
	 *
	 * @return True if there is an error and it is handled
	 */
	private boolean handleError(Exception exception) {
		return exception != null &&
					(twoParameterOnErrorListener != null && twoParameterOnErrorListener.call(this, exception)) |
					(oneParameterOnErrorListener != null && oneParameterOnErrorListener.call(exception));

	}

	public FluentTask<TParams, TProgress, TResult> cancel() {
		return cancel(true);
	}

	public FluentTask<TParams, TProgress, TResult> cancel(boolean interrupt) {
		task.getObject().cancel(interrupt);
		return this;
	}

	public boolean isCancelled() {
		return task.getObject().isCancelled();
	}

	protected abstract TResult executeInBackground(TParams[] params);

	protected void setException(Exception exception) {
		task.getObject().setException(exception);
	}

	public FluentTask<TParams, TProgress, TResult> onComplete(TwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> listener) {
		twoParameterOnCompleteListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onComplete(OneParameterRunnable<TResult> listener) {
		oneParameterOnCompleteListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onError(TwoParameterCallable<FluentTask<TParams, TProgress, TResult>, Exception, Boolean> listener) {
		twoParameterOnErrorListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onError(OneParameterCallable<Exception, Boolean> listener) {
		oneParameterOnErrorListener = listener;
		return this;
	}
}
