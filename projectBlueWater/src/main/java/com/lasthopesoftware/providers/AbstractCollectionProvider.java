package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractCollectionProvider<T> {
	private final ConnectionProvider connectionProvider;
	private OnCompleteListener<Void, Void, List<T>> onGetItemsComplete;
    private OnErrorListener<Void, Void, List<T>> onGetItemsError;
    private final String[] params;
    private static final ExecutorService collectionAccessExecutor = Executors.newSingleThreadExecutor();
    private Exception exception = null;
    private SimpleTask<Void, Void, List<T>> task;
	
	protected AbstractCollectionProvider(ConnectionProvider connectionProvider, String... params) {
        this.connectionProvider = connectionProvider;
		this.params = params;
		
	}
	
	public AbstractCollectionProvider<T> onComplete(OnCompleteListener<Void, Void, List<T>> onGetItemsComplete) {
		this.onGetItemsComplete = onGetItemsComplete;
		return this;
	}
	
	public AbstractCollectionProvider<T> onError(OnErrorListener<Void, Void, List<T>> onGetItemsError) {
		this.onGetItemsError = onGetItemsError;
		return this;
	}
	
	public void execute() {
		execute(collectionAccessExecutor);
	}
	
	public void execute(Executor executor) {
        getTask().execute(executor);
	}
	
	public List<T> get() throws ExecutionException, InterruptedException {
		return get(collectionAccessExecutor);
	}
	
	public List<T> get(Executor executor) throws ExecutionException, InterruptedException {
		return getTask().execute(executor).get();
	}

    public void cancel(boolean mayInterrupt) {
        getTask().cancel(mayInterrupt);
    }

    private SimpleTask<Void, Void, List<T>> getTask() {
        if (task != null) return task;

        task = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, List<T>>() {

            @Override
            public List<T> onExecute(ISimpleTask<Void, Void, List<T>> owner, Void... voidParams) throws Exception {
                if (owner.isCancelled()) return new ArrayList<>();

                final HttpURLConnection connection = connectionProvider.getConnection(params);
	            try {
		            return getCollection(owner, connectionProvider.getConnection(params));
	            } finally {
		            connection.disconnect();
	            }
            }
        });

        task.addOnErrorListener(new ISimpleTask.OnErrorListener<Void, Void, List<T>>() {
            @Override
            public boolean onError(ISimpleTask<Void, Void, List<T>> owner, boolean isHandled, Exception innerException) {
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

	protected abstract List<T> getCollection(ISimpleTask<Void, Void, List<T>> task, HttpURLConnection connection) throws Exception;

    public Exception getException() {
        return exception;
    }

    protected void setException(Exception exception) {
        this.exception = exception;
    }
}
