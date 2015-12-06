package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.callables.IThreeParameterCallable;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.Lazy;
import com.lasthopesoftware.threading.OnExecuteListener;

import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 11/26/15.
 */
public abstract class AbstractProvider<T> {
	private final ConnectionProvider connectionProvider;
	private ITwoParameterRunnable<FluentTask<Void, Void, T>, T> onGetItemsComplete;
	private IThreeParameterCallable<FluentTask<Void, Void, T>, Boolean, Exception, Boolean> onGetItemsError;
	private final String[] params;
	private static final ExecutorService collectionAccessExecutor = Executors.newSingleThreadExecutor();
	private Exception exception = null;
	private final Lazy<FluentTask<Void, Void, T>> task = new Lazy<>(new Callable<FluentTask<Void, Void, T>>() {
		@Override
		public FluentTask<Void, Void, T> call() throws Exception {
			final FluentTask<Void, Void, T> task = new FluentTask<>(new OnExecuteListener<Void, Void, T>() {

				@Override
				public T onExecute(FluentTask<Void, Void, T> owner, Void... voidParams) throws Exception {
					if (owner.isCancelled()) return null;

					final HttpURLConnection connection = connectionProvider.getConnection(params);
					try {
						return getData(owner, connectionProvider.getConnection(params));
					} finally {
						connection.disconnect();
					}
				}
			});

			task.onError(new IThreeParameterCallable<FluentTask<Void, Void, T>, Boolean, Exception, Boolean>() {
				@Override
				public Boolean call(FluentTask<Void, Void, T> owner, Boolean isHandled, Exception innerException) {
					setException(innerException);
					return false;
				}
			});

			if (onGetItemsComplete != null)
				task.onComplete(onGetItemsComplete);

			if (onGetItemsError != null)
				task.onError(onGetItemsError);

			return task;
		}
	});

	protected AbstractProvider(ConnectionProvider connectionProvider, String... params) {
		this.connectionProvider = connectionProvider;
		this.params = params;

	}

	public AbstractProvider<T> onComplete(ITwoParameterRunnable<FluentTask<Void, Void, T>, T> onGetItemsComplete) {
		this.onGetItemsComplete = onGetItemsComplete;
		return this;
	}

	public AbstractProvider<T> onError(IThreeParameterCallable<FluentTask<Void, Void, T>, Boolean, Exception, Boolean> onGetItemsError) {
		this.onGetItemsError = onGetItemsError;
		return this;
	}

	public void execute() {
		execute(collectionAccessExecutor);
	}

	public void execute(Executor executor) {
		task.getObject().execute(executor);
	}

	public T get() throws ExecutionException, InterruptedException {
		return get(collectionAccessExecutor);
	}

	private T get(Executor executor) throws ExecutionException, InterruptedException {
		return task.getObject().execute(AbstractProvider.collectionAccessExecutor).get();
	}

	public void cancel() {
		task.getObject().cancel(true);
	}

	protected abstract T getData(FluentTask<Void, Void, T> task, HttpURLConnection connection) throws Exception;

	public Exception getException() {
		return exception;
	}

	private void setException(Exception exception) {
		this.exception = exception;
	}
}
