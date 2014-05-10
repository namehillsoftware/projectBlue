package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.JrDataTask;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;


public abstract class JrItemAsyncBase<T extends IJrItem<?>> extends JrObject implements IJrItem<T>, IJrItemAsync<T>, Comparable<T> {
	protected ArrayList<T> mSubItems;
	
	public JrItemAsyncBase(int key, String value) {
		super(key, value);
	}
	
	public JrItemAsyncBase(String value) {
		super(value);
	}
	
	public JrItemAsyncBase() {
	}
	
	/* Required Methods for Sub Item Async retrieval */
	protected abstract String[] getSubItemParams();
	public abstract void setOnItemsCompleteListener(OnCompleteListener<List<T>> listener);
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
		
		if (mSubItems == null || mSubItems.size() == 0) {
			try {
				// This will call the onCompletes if they are attached.
				JrDataTask<List<T>> getNewSubItemsTask = getNewSubItemsTask();
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
				LoggerFactory.getLogger(JrItemAsyncBase.class).error(e.toString(), e);
			}
		}
		
		return mSubItems;
	}
	
	public void getSubItemsAsync() {
		
		if (mSubItems == null || mSubItems.size() == 0) {
			final JrDataTask<List<T>>  itemTask = getNewSubItemsTask();
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
		final SimpleTask<String, Void, List<T>> task = new SimpleTask<String, Void, List<T>>();
		
		if (getOnItemsCompleteListeners() != null) {
			for (OnCompleteListener<List<T>> listener : getOnItemsCompleteListeners())
				task.addOnCompleteListener(listener);
		}
		
		task.setOnExecuteListener(new OnExecuteListener<String, Void, List<T>>() {

			@Override
			public List<T> onExecute(ISimpleTask<String, Void, List<T>> owner, String... params) throws Exception {
				return mSubItems;
			}
		});
		
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	@SuppressWarnings("unchecked")
	protected JrDataTask<List<T>> getNewSubItemsTask() {
		final JrDataTask<List<T>> subItemsTask = new JrDataTask<List<T>>();

		if (getOnItemsCompleteListeners() != null) {
			for (OnCompleteListener<List<T>> listener : getOnItemsCompleteListeners()) subItemsTask.addOnCompleteListener(listener);
		}
			
		if (getOnItemsStartListeners() != null) {
			for (OnStartListener<List<T>> listener : getOnItemsStartListeners()) subItemsTask.addOnStartListener(listener);
		}
		
		subItemsTask.addOnConnectListener(getOnItemConnectListener());
		
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
