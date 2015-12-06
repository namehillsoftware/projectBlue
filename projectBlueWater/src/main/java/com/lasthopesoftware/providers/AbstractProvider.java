package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.IFluentTask;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 11/26/15.
 */
public abstract class AbstractProvider<T> {
	private final ConnectionProvider connectionProvider;
	private IFluentTask.OnCompleteListener<Void, Void, T> onGetItemsComplete;
	private IFluentTask.OnErrorListener<Void, Void, T> onGetItemsError;
	private final String[] params;
	private static final ExecutorService collectionAccessExecutor = Executors.newSingleThreadExecutor();
	private Exception exception = null;
	private FluentTask<Void, Void, T> task;

	protected AbstractProvider(ConnectionProvider connectionProvider, String... params) {
		this.connectionProvider = connectionProvider;
		this.params = params;

	}

	public AbstractProvider<T> onComplete(IFluentTask.OnCompleteListener<Void, Void, T> onGetItemsComplete) {
		this.onGetItemsComplete = onGetItemsComplete;
		return this;
	}

	public AbstractProvider<T> onError(IFluentTask.OnErrorListener<Void, Void, T> onGetItemsError) {
		this.onGetItemsError = onGetItemsError;
		return this;
	}

	public void execute() {
		execute(collectionAccessExecutor);
	}

	public void execute(Executor executor) {
		getTask().execute(executor);
	}

	public T get() throws ExecutionException, InterruptedException {
		return get(collectionAccessExecutor);
	}

	private T get(Executor executor) throws ExecutionException, InterruptedException {
		return getTask().execute(AbstractProvider.collectionAccessExecutor).get();
	}

	public void cancel() {
		getTask().cancel(true);
	}

	private FluentTask<Void, Void, T> getTask() {
		if (task != null) return task;

		task = new FluentTask<>(new IFluentTask.OnExecuteListener<Void, Void, T>() {

			@Override
			public T onExecute(IFluentTask<Void, Void, T> owner, Void... voidParams) throws Exception {
				if (owner.isCancelled()) return null;

				final HttpURLConnection connection = connectionProvider.getConnection(params);
				try {
					return getData(owner, connectionProvider.getConnection(params));
				} finally {
					connection.disconnect();
				}
			}
		});

		task.addOnErrorListener(new IFluentTask.OnErrorListener<Void, Void, T>() {
			@Override
			public boolean onError(IFluentTask<Void, Void, T> owner, boolean isHandled, Exception innerException) {
				setException(innerException);
				return false;
			}
		});

		if (onGetItemsComplete != null)
			task.addOnCompleteListener(onGetItemsComplete);

		if (onGetItemsError != null)
			task.addOnErrorListener(onGetItemsError);

		return task;
	}

	protected abstract T getData(IFluentTask<Void, Void, T> task, HttpURLConnection connection) throws Exception;

	public Exception getException() {
		return exception;
	}

	private void setException(Exception exception) {
		this.exception = exception;
	}
}
