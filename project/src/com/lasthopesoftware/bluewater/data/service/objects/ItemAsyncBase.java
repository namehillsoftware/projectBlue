package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.DataTask;
import com.lasthopesoftware.bluewater.data.service.access.RevisionChecker;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;


public abstract class ItemAsyncBase<T extends IItem<?>> extends BaseObject implements IItem<T>, IItemAsync<T>, Comparable<T> {
	private final static Logger mLogger = LoggerFactory.getLogger(ItemAsyncBase.class);
	
	protected ArrayList<T> mSubItems;
	
	public ItemAsyncBase(int key, String value) {
		super(key, value);
	}
	
	public ItemAsyncBase(String value) {
		super(value);
	}
	
	public ItemAsyncBase() {
	}
	
	/* Required Methods for Sub Item Async retrieval */
	protected abstract String[] getSubItemParams();
	public abstract void addOnItemsCompleteListener(OnCompleteListener<List<T>> listener);
	public abstract void removeOnItemsCompleteListener(OnCompleteListener<List<T>> listener);
	protected abstract OnConnectListener<List<T>> getOnItemConnectListener();
	protected abstract List<OnCompleteListener<List<T>>> getOnItemsCompleteListeners();
	protected abstract List<OnStartListener<List<T>>> getOnItemsStartListeners();
	protected abstract List<OnErrorListener<List<T>>> getOnItemsErrorListeners();
	
	@SuppressWarnings("rawtypes")
	private static ISimpleTask.OnErrorListener onIoExceptionHandler = new ISimpleTask.OnErrorListener() {

		@Override
		public boolean onError(ISimpleTask owner, Exception innerException) {
			return !(innerException instanceof IOException);
		}
	};
	
	public ArrayList<T> getSubItems() throws IOException {
		try {
			if (RevisionChecker.getIsNewRevisionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get() == Boolean.TRUE)
				mSubItems = null;
		} catch (InterruptedException | ExecutionException e) {
			mLogger.error(e.toString(), e);
			mSubItems = null;
		}
		
		if (mSubItems == null || mSubItems.size() == 0) {
			try {
				// This will call the onCompletes if they are attached.
				DataTask<List<T>> getNewSubItemsTask = getNewSubItemsTask();
				List<T> result = getNewSubItemsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getSubItemParams()).get();
				
				if (result != null)
					mSubItems = new ArrayList<T>(result);
				else
					mSubItems = new ArrayList<T>();
				
				if (getNewSubItemsTask.getState() == SimpleTaskState.ERROR) {
					for (Exception exception : getNewSubItemsTask.getExceptions()) {
						if (exception instanceof IOException)
							throw exception;
					}
				}
			} catch(IOException ioE) {
				throw ioE;
			} catch(Exception e) {
				mLogger.error(e.toString(), e);
			}
		}
		
		return mSubItems;
	}
	
	public void getSubItemsAsync() {
		final SimpleTask<Void, Void, Boolean> isNewRevisionTask = RevisionChecker.getIsNewRevisionTask();
		isNewRevisionTask.addOnCompleteListener(new ISimpleTask.OnCompleteListener<Void, Void, Boolean>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, Boolean> owner, Boolean result) {
				if (result == Boolean.TRUE || mSubItems == null || mSubItems.size() == 0) {
					final DataTask<List<T>>  itemTask = getNewSubItemsTask();
					itemTask.addOnCompleteListener(new OnCompleteListener<List<T>>() {

						@Override
						public void onComplete(ISimpleTask<String, Void, List<T>> owner, List<T> result) {
							if (owner.getState() == SimpleTaskState.ERROR || result == null) {
								mSubItems = new ArrayList<T>();
								return;
							}
							mSubItems = new ArrayList<T>(result);
						}
						
					});
					
					itemTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getSubItemParams());
					return;
				}
				
				// Simple task that just returns sub items if they are in memory
				final SimpleTask<String, Void, List<T>> task = new SimpleTask<String, Void, List<T>>(new OnExecuteListener<String, Void, List<T>>() {

					@Override
					public List<T> onExecute(ISimpleTask<String, Void, List<T>> owner, String... params) throws Exception {
						return mSubItems;
					}
				});
				
				if (getOnItemsCompleteListeners() != null) {
					for (OnCompleteListener<List<T>> listener : getOnItemsCompleteListeners())
						task.addOnCompleteListener(listener);
				}
				
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});
		
		isNewRevisionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	@SuppressWarnings("unchecked")
	protected DataTask<List<T>> getNewSubItemsTask() {
		final DataTask<List<T>> subItemsTask = new DataTask<List<T>>(getOnItemConnectListener());

		if (getOnItemsCompleteListeners() != null) {
			for (OnCompleteListener<List<T>> listener : getOnItemsCompleteListeners()) subItemsTask.addOnCompleteListener(listener);
		}
			
		if (getOnItemsStartListeners() != null) {
			for (OnStartListener<List<T>> listener : getOnItemsStartListeners()) subItemsTask.addOnStartListener(listener);
		}
		
		if (getOnItemsErrorListeners() != null) {
			for (OnErrorListener<List<T>> listener : getOnItemsErrorListeners()) subItemsTask.addOnErrorListener(listener);
		}
		
		subItemsTask.addOnErrorListener(onIoExceptionHandler);
		
		return subItemsTask;
	}
	

	@Override
	public int compareTo(T another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey() - another.getKey();
		return result;
	}
}
