package com.lasthopesoftware.bluewater.servers.library.items.access;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.SimpleTask;

public abstract class AbstractCollectionProvider<T extends IItem> {
	protected final HttpURLConnection mConnection;
	protected OnCompleteListener<Void, Void, List<T>> mOnGetItemsComplete;
	protected OnErrorListener<Void, Void, List<T>> mOnGetItemsError;
	protected final String[] mParams;
	private static final ExecutorService mCollectionAccessExecutor = Executors.newSingleThreadExecutor(); 

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
		getNewTask().execute(executor);
	}
	
	public List<T> get() throws ExecutionException, InterruptedException {
		return get(mCollectionAccessExecutor);
	}
	
	public List<T> get(Executor executor) throws ExecutionException, InterruptedException {
		return getNewTask().execute(executor).get();
	}
	
	protected abstract SimpleTask<Void, Void, List<T>> getNewTask();
}
