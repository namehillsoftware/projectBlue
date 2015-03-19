package com.lasthopesoftware.bluewater.servers.library.items.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractCollectionProvider<T extends IItem> {
	protected final HttpURLConnection mConnection;
	protected OnCompleteListener<Void, Void, List<T>> mOnGetItemsComplete;
	protected OnErrorListener<Void, Void, List<T>> mOnGetItemsError;
	private final String[] mParams;
	private static final ExecutorService mCollectionAccessExecutor = Executors.newSingleThreadExecutor();
    private Exception mException = null;
    private SimpleTask<Void, Void, List<T>> mTask;

	public AbstractCollectionProvider(String... params) {
		this(null, params);
	}
	
	public AbstractCollectionProvider(HttpURLConnection connection, String... params) {
		mConnection = connection;
		mParams = params;
		
	}
	
	public AbstractCollectionProvider<T> onComplete(OnCompleteListener<Void, Void, List<T>> onGetItemsComplete) {
		mOnGetItemsComplete = onGetItemsComplete;
		return this;
	}
	
	public AbstractCollectionProvider<T> onError(OnErrorListener<Void, Void, List<T>> onGetItemsError) {
		mOnGetItemsError = onGetItemsError;
		return this;
	}
	
	public void execute() {
		execute(mCollectionAccessExecutor);
	}
	
	public void execute(Executor executor) {
        getTask().execute(executor);
	}
	
	public List<T> get() throws ExecutionException, InterruptedException {
		return get(mCollectionAccessExecutor);
	}
	
	public List<T> get(Executor executor) throws ExecutionException, InterruptedException {
		return getTask().execute(executor).get();
	}

    public void cancel(boolean mayInterrupt) {
        getTask().cancel(mayInterrupt);
    }

    private SimpleTask<Void, Void, List<T>> getTask() {
        if (mTask != null) return mTask;

        mTask = new SimpleTask<Void, Void, List<T>>(new ISimpleTask.OnExecuteListener<Void, Void, List<T>>() {

            @Override
            public List<T> onExecute(ISimpleTask<Void, Void, List<T>> owner, Void... voidParams) throws Exception {
                if (owner.isCancelled()) return new ArrayList<>();
                final HttpURLConnection conn = mConnection == null ? ConnectionProvider.getConnection(mParams) : mConnection;
                try {
                    if (owner.isCancelled()) return new ArrayList<>();
                    return getItems(conn, mParams);
                } finally {
                    if (mConnection == null) conn.disconnect();
                }
            }
        });

        mTask.addOnErrorListener(new ISimpleTask.OnErrorListener<Void, Void, List<T>>() {
            @Override
            public boolean onError(ISimpleTask<Void, Void, List<T>> owner, boolean isHandled, Exception innerException) {
                setException(innerException);
                return false;
            }
        });

        if (mOnGetItemsComplete != null)
            mTask.addOnCompleteListener(mOnGetItemsComplete);

        if (mOnGetItemsError != null)
            mTask.addOnErrorListener(mOnGetItemsError);

        return mTask;
    }

	protected abstract List<T> getItems(HttpURLConnection connection, String... params) throws Exception;

    public Exception getException() {
        return mException;
    }

    protected void setException(Exception exception) {
        mException = exception;
    }
}
