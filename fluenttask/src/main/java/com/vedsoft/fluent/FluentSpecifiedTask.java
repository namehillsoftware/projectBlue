package com.vedsoft.fluent;

import android.os.AsyncTask;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.callables.TwoParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;
import com.vedsoft.lazyj.AbstractSynchronousLazy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class FluentSpecifiedTask<TParams, TProgress, TResult> implements IFluentTask<TParams, TProgress, TResult> {

	private final TParams[] params;
	private final Executor defaultExecutor;

	private OneParameterRunnable<IFluentTask<TParams, TProgress, TResult>> oneParameterBeforeStartListener;
	private Runnable beforeStartListener;

	private TwoParameterRunnable<IFluentTask<TParams, TProgress, TResult>, TProgress[]> twoParameterOnProgressListener;
	private OneParameterRunnable<TProgress[]> oneParameterOnProgressListener;

	private ThreeParameterRunnable<IFluentTask<TParams, TProgress, TResult>, TResult, Exception> threeParameterOnCompleteListener;
	private TwoParameterRunnable<TResult, Exception> twoParameterOnCompleteListener;
	private OneParameterRunnable<TResult> oneParameterOnCompleteListener;

	private OneParameterCallable<Exception, Boolean> oneParameterOnErrorListener;
	private TwoParameterCallable<IFluentTask<TParams, TProgress, TResult>, Exception, Boolean> twoParameterOnErrorListener;

	private volatile boolean isExecuting = false;

	private final AbstractSynchronousLazy<AndroidAsyncTask<Void, TProgress, TResult>> task = new AbstractSynchronousLazy<AndroidAsyncTask<Void, TProgress, TResult>>() {
		@Override
		protected final AndroidAsyncTask<Void, TProgress, TResult> initialize() {
			return new AndroidAsyncTask<Void, TProgress, TResult>(){

				@Override
				protected final void onPreExecute() {
					if (oneParameterBeforeStartListener != null)
						oneParameterBeforeStartListener.run(FluentSpecifiedTask.this);

					if (beforeStartListener != null)
						beforeStartListener.run();
				}

				@Override
				protected final TResult doInBackground(Void... params) {
					return executeInBackground(FluentSpecifiedTask.this.params);
				}

				@Override
				protected final void onProgressUpdate(TProgress... values) {
					if (twoParameterOnProgressListener != null)
						twoParameterOnProgressListener.run(FluentSpecifiedTask.this, values);

					if (oneParameterOnProgressListener != null)
						oneParameterOnProgressListener.run(values);
				}

				@Override
				protected final void onPostExecute(TResult result, Exception exception) {
					handleError(exception);

					if (threeParameterOnCompleteListener != null)
						threeParameterOnCompleteListener.run(FluentSpecifiedTask.this, result, exception);

					if (twoParameterOnCompleteListener != null)
						twoParameterOnCompleteListener.run(result, exception);

					if (oneParameterOnCompleteListener != null)
						oneParameterOnCompleteListener.run(result);
				}

				@Override
				protected final void onCancelled(TResult result, Exception exception) {
					handleError(exception);
				}
			};
		}
	};

	@SafeVarargs
	public FluentSpecifiedTask(TParams... params) {
		this(AsyncTask.SERIAL_EXECUTOR, params);
	}

	@SafeVarargs
	public FluentSpecifiedTask(Executor defaultExecutor, TParams... params) {
		this.params = params;
		this.defaultExecutor = defaultExecutor;
	}

	public IFluentTask<TParams, TProgress, TResult> execute() {
		return execute(null);
	}

	public IFluentTask<TParams, TProgress, TResult> execute(Executor exec) {
		executeTask(exec);
		return this;
	}

	public TResult get() throws ExecutionException, InterruptedException {
		return get(null);
	}

	public TResult get(long timeout, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
		return get(null, timeout, timeUnit);
	}

	public TResult get(Executor executor) throws ExecutionException, InterruptedException {
		if (!isExecuting)
			executeTask(executor);

		final TResult result = task.getObject().get();

		throwOnTaskException(task.getObject());

		return result;
	}

	public TResult get(Executor executor, long timeout, TimeUnit timeUnit) throws TimeoutException, ExecutionException, InterruptedException {
		if (!isExecuting)
			executeTask(executor);

		final TResult result = task.getObject().get(timeout, timeUnit);

		throwOnTaskException(task.getObject());

		return result;
	}

	private static <TParams, TProgress, TResult> void throwOnTaskException(AndroidAsyncTask<TParams, TProgress, TResult> task) throws ExecutionException {
		final Exception exception = task.getException();
		if (exception != null)
			throw new ExecutionException(exception);
	}

	private synchronized AsyncTask<Void, TProgress, TResult> executeTask(Executor exec) {
		isExecuting = true;
		return task.getObject().executeOnExecutor(exec != null ? exec : defaultExecutor);
	}

	protected void reportProgress(TProgress... progress) {
		task.getObject().updateProgress(progress);
	}

	private void handleError(Exception exception) {
		if (exception == null) return;

		if (twoParameterOnErrorListener != null)
			twoParameterOnErrorListener.call(this, exception);

		if (oneParameterOnErrorListener != null)
			oneParameterOnErrorListener.call(exception);
	}

	public FluentSpecifiedTask<TParams, TProgress, TResult> cancel() {
		return cancel(true);
	}

	public FluentSpecifiedTask<TParams, TProgress, TResult> cancel(boolean interrupt) {
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

	@Override
	public IFluentTask<TParams, TProgress, TResult> beforeStart(OneParameterRunnable<IFluentTask<TParams, TProgress, TResult>> listener) {
		oneParameterBeforeStartListener = listener;
		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> beforeStart(Runnable listener) {
		beforeStartListener = listener;
		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onComplete(ThreeParameterRunnable<IFluentTask<TParams, TProgress, TResult>, TResult, Exception> listener) {
		threeParameterOnCompleteListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED) {
			try {
				listener.run(this, task.getObject().get(), task.getObject().getException());
			} catch (InterruptedException | ExecutionException ignored) {
			}
		}

		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onComplete(TwoParameterRunnable<TResult, Exception> listener) {
		twoParameterOnCompleteListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED) {
			try {
				listener.run(task.getObject().get(), task.getObject().getException());
			} catch (InterruptedException | ExecutionException ignored) {
			}
		}

		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onComplete(OneParameterRunnable<TResult> listener) {
		oneParameterOnCompleteListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED) {
			try {
				listener.run(task.getObject().get());
			} catch (InterruptedException | ExecutionException ignored) {
			}
		}

		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onProgress(TwoParameterRunnable<IFluentTask<TParams, TProgress, TResult>, TProgress[]> listener) {
		twoParameterOnProgressListener = listener;
		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onProgress(OneParameterRunnable<TProgress[]> listener) {
		oneParameterOnProgressListener = listener;
		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onError(TwoParameterCallable<IFluentTask<TParams, TProgress, TResult>, Exception, Boolean> listener) {
		twoParameterOnErrorListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED)
			handleError(task.getObject().getException());

		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onError(OneParameterCallable<Exception, Boolean> listener) {
		oneParameterOnErrorListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED)
			handleError(task.getObject().getException());

		return this;
	}

	private static abstract class AndroidAsyncTask<TParams, TProgress, TResult> extends AsyncExceptionTask<TParams, TProgress, TResult> {
		void updateProgress(TProgress[] progress) {
			publishProgress(progress);
		}
	}
}
