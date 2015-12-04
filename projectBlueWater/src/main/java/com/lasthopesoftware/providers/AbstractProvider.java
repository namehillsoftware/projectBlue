package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

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
	private ISimpleTask.OnCompleteListener<Void, Void, T> onGetItemsComplete;
	private ISimpleTask.OnErrorListener<Void, Void, T> onGetItemsError;
	private final String[] params;
	private static final ExecutorService collectionAccessExecutor = Executors.newSingleThreadExecutor();
	private Exception exception = null;
	private SimpleTask<Void, Void, T> task;

	protected AbstractProvider(ConnectionProvider connectionProvider, String... params) {
		this.connectionProvider = connectionProvider;
		this.params = params;

	}

	public AbstractProvider<T> onComplete(ISimpleTask.OnCompleteListener<Void, Void, T> onGetItemsComplete) {
		this.onGetItemsComplete = onGetItemsComplete;
		return this;
	}

	public AbstractProvider<T> onError(ISimpleTask.OnErrorListener<Void, Void, T> onGetItemsError) {
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

	private SimpleTask<Void, Void, T> getTask() {
		if (task != null) return task;

		task = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, T>() {

			@Override
			public T onExecute(ISimpleTask<Void, Void, T> owner, Void... voidParams) throws Exception {
				if (owner.isCancelled()) return null;

				final HttpURLConnection connection = connectionProvider.getConnection(params);
				try {
					return getData(owner, connectionProvider.getConnection(params));
				} finally {
					connection.disconnect();
				}
			}
		});

		task.addOnErrorListener(new ISimpleTask.OnErrorListener<Void, Void, T>() {
			@Override
			public boolean onError(ISimpleTask<Void, Void, T> owner, boolean isHandled, Exception innerException) {
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

	protected abstract T getData(ISimpleTask<Void, Void, T> task, HttpURLConnection connection) throws Exception;

	public Exception getException() {
		return exception;
	}

	private void setException(Exception exception) {
		this.exception = exception;
	}
}
