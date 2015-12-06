package com.lasthopesoftware.threading;

import android.os.AsyncTask;

import com.lasthopesoftware.callables.IOneParameterCallable;
import com.lasthopesoftware.callables.ITwoParameterCallable;
import com.lasthopesoftware.runnables.IOneParameterRunnable;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public abstract class FluentTask<TParams, TProgress, TResult>  {

	private final TParams[] params;

	private ITwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> twoParameterOnCompleteListener;
	private IOneParameterRunnable<TResult> oneParameterOnCompleteListener;

	private IOneParameterCallable<Exception, Boolean> oneParameterOnErrorListener;
	private ITwoParameterCallable<FluentTask<TParams, TProgress, TResult>, Exception, Boolean> twoParameterOnErrorListener;

	private final Lazy<AsyncExceptionTask<Void, TProgress, TResult>> task = new Lazy<>(new Callable<AsyncExceptionTask<Void, TProgress, TResult>>()
	{

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
		this.params = params;
	}

	public void execute() {
		execute(AsyncTask.SERIAL_EXECUTOR);
	}

	public void execute(Executor exec) {
		task.getObject().executeOnExecutor(exec);
	}

	public TResult get() throws ExecutionException, InterruptedException {
		return get(AsyncTask.SERIAL_EXECUTOR);
	}

	public TResult get(Executor executor) throws ExecutionException, InterruptedException {
		return task.getObject().executeOnExecutor(executor).get();
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
