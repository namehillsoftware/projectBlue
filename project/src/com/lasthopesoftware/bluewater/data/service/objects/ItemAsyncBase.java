package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lasthopesoftware.bluewater.data.service.access.DataTask;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;


public abstract class ItemAsyncBase<T extends IItem<?>> extends AbstractIntKeyStringValue implements IItem<T>, IItemAsync<T>, Comparable<T> {
	private final static Logger mLogger = LoggerFactory.getLogger(ItemAsyncBase.class);
	
	protected ArrayList<T> mSubItems;
	
	private final Object syncObj = new Object();
	
	// Ensure that getting sub items only happens one at a time
	private final static ExecutorService mSubItemExecutor = Executors.newSingleThreadExecutor();
	
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
			
	public ArrayList<T> getSubItems() throws IOException {
		synchronized(syncObj) {
			if (mSubItems == null || mSubItems.size() == 0) {
				try {
					// This will call the onCompletes if they are attached.
					DataTask<List<T>> getNewSubItemsTask = getNewSubItemsTask();
					List<T> result = getNewSubItemsTask.execute(mSubItemExecutor, getSubItemParams()).get();
					
					if (getNewSubItemsTask.getState() == SimpleTaskState.ERROR && getNewSubItemsTask.getException() instanceof IOException)
						throw new IOException(getNewSubItemsTask.getException());
					
					mSubItems = result != null ? new ArrayList<T>(result) : new ArrayList<T>();
				} catch(Exception e) {
					mLogger.error(e.toString(), e);
				}
			}
		}
		
		return mSubItems;
	}
	
	public void getSubItemsAsync() {
		synchronized(syncObj) {
			if (mSubItems == null || mSubItems.size() == 0) {
				final DataTask<List<T>>  itemTask = getNewSubItemsTask();
				itemTask.addOnCompleteListener(new OnCompleteListener<List<T>>() {
	
					@Override
					public void onComplete(ISimpleTask<String, Void, List<T>> owner, List<T> result) {
						synchronized(syncObj)  {
							if (owner.getState() == SimpleTaskState.ERROR || result == null) {
								mSubItems = new ArrayList<T>();
								return;
							}
							mSubItems = new ArrayList<T>(result);
						}
					}
					
				});
				
				itemTask.execute(mSubItemExecutor, getSubItemParams());
				return;
			}
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
		task.execute(mSubItemExecutor);
	}
	
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
		
		return subItemsTask;
	}
	

	@Override
	public int compareTo(T another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey() - another.getKey();
		return result;
	}
}
