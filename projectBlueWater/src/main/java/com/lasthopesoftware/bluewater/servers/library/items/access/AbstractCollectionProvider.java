package com.lasthopesoftware.bluewater.servers.library.items.access;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractCollectionProvider<TParam extends IItem, TResult extends IItem> {
	protected final HttpURLConnection mConnection;
	protected OnCompleteListener<Void, Void, List<TResult>> mOnGetItemsComplete;
	protected OnErrorListener<Void, Void, List<TResult>> mOnGetItemsError;
	private static final ExecutorService mCollectionAccessExecutor = Executors.newSingleThreadExecutor();
    private Exception mException = null;
    private SimpleTask<Void, Void, List<TResult>> mTask;
    private final TParam mItem;

    public AbstractCollectionProvider(TParam item) {
        this(null, item);
    }

	public AbstractCollectionProvider(HttpURLConnection connection, TParam item) {
		mConnection = connection;
        mItem = item;
	}
	
	public AbstractCollectionProvider<TParam, TResult> onComplete(OnCompleteListener<Void, Void, List<TResult>> onGetItemsComplete) {
		mOnGetItemsComplete = onGetItemsComplete;
		return this;
	}
	
	public AbstractCollectionProvider<TParam, TResult> onError(OnErrorListener<Void, Void, List<TResult>> onGetItemsError) {
		mOnGetItemsError = onGetItemsError;
		return this;
	}
	
	public void execute() {
		execute(mCollectionAccessExecutor);
	}
	
	public void execute(Executor executor) {
        getTask().execute(executor);
	}
	
	public List<TResult> get() throws ExecutionException, InterruptedException {
		return get(mCollectionAccessExecutor);
	}
	
	public List<TResult> get(Executor executor) throws ExecutionException, InterruptedException {
		return getTask().execute(executor).get();
	}

    public void cancel(boolean mayInterrupt) {
        getTask().cancel(mayInterrupt);
    }

    private SimpleTask<Void, Void, List<TResult>> getTask() {
        if (mTask == null) mTask = buildTask(mItem);
        return mTask;
    }

	protected abstract SimpleTask<Void, Void, List<TResult>> buildTask(final TParam intKeyStringValue);

    public Exception getException() {
        return mException;
    }

    protected void setException(Exception exception) {
        mException = exception;
    }
}
