package com.lasthopesoftware.bluewater.servers.library.items;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;

public abstract class AbstractIItemProvider {
	protected final HttpURLConnection mConnection;
	protected OnCompleteListener<Void, Void, List<IItem>> mOnGetItemsComplete;
	protected OnErrorListener<Void, Void, List<IItem>> mOnGetItemsError;
	protected final String[] mParams;
	
	public AbstractIItemProvider(String... params) {
		this(null, params);
	}
	
	public AbstractIItemProvider(HttpURLConnection connection, String... params) {
		mConnection = connection;
		mParams = params;
		
	}
	
	public AbstractIItemProvider onComplete(OnCompleteListener<Void, Void, List<IItem>> onGetItemsComplete) {
		mOnGetItemsComplete = onGetItemsComplete;
		return this;
	}
	
	public AbstractIItemProvider onError(OnErrorListener<Void, Void, List<IItem>> onGetItemsError) {
		mOnGetItemsError = onGetItemsError;
		return this;
	}
	
	public void execute() {
		execute(AsyncTask.SERIAL_EXECUTOR);
	}
	
	public void execute(Executor executor) {
		getNewTask().execute(executor);
	}
	
	public List<IItem> get() throws ExecutionException, InterruptedException {
		return get(AsyncTask.SERIAL_EXECUTOR);
	}
	
	public List<IItem> get(Executor executor) throws ExecutionException, InterruptedException {
		return getNewTask().execute(executor).get();
	}
	
	protected abstract SimpleTask<Void, Void, List<IItem>> getNewTask();
}
