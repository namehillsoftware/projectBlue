package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.JrDataTask;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.threading.ISimpleTask;


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
	
	public ArrayList<T> getSubItems() {
		JrDataTask<List<T>> itemTask = getNewSubItemsTask();
		
		if (mSubItems == null) {
			try {
				// This will call the onCompletes if they are attached.
				mSubItems = (ArrayList<T>) itemTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getSubItemParams()).get();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSubItems;
	}
	
	public void getSubItemsAsync() {
		JrDataTask<List<T>> itemTask = new JrDataTask<List<T>>();
		
		if (mSubItems == null) {
			itemTask = getNewSubItemsTask();
			itemTask.addOnCompleteListener(new OnCompleteListener<List<T>>() {

				@Override
				public void onComplete(ISimpleTask<String, Void, List<T>> owner, List<T> result) {
					mSubItems = (ArrayList<T>) result;
				}
				
			});
			
			itemTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getSubItemParams());
			return;
		}
		
		for (OnCompleteListener<List<T>> listener : getOnItemsCompleteListeners()) listener.onComplete(itemTask, mSubItems);
	}
	
	protected JrDataTask<List<T>> getNewSubItemsTask() {
		JrDataTask<List<T>> subItemsTask = new JrDataTask<List<T>>();

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
		
		return subItemsTask;
	}
	

	@Override
	public int compareTo(T another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey().compareTo(another.getKey());
		return result;
	}
}
