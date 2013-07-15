package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;

import jrAccess.JrDataTask;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnStartListener;

public abstract class JrItemAsyncBase<T extends JrObject> extends JrObject implements IJrItem<T>, IJrItemAsync {
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
	public abstract void setOnItemsCompleteListener(IJrDataTask.OnCompleteListener<List<T>> listener);
	protected abstract OnConnectListener<List<T>> getOnItemConnectListener();
	protected abstract List<IJrDataTask.OnCompleteListener<List<T>>> getOnItemsCompleteListeners();
	protected abstract List<IJrDataTask.OnStartListener> getOnItemsStartListeners();
	protected abstract List<IJrDataTask.OnErrorListener> getOnItemsErrorListeners();
	
	public ArrayList<T> getSubItems() {
		JrDataTask<List<T>> itemTask = getNewSubItemsTask();
		
		if (mSubItems == null) {
			try {
				mSubItems = (ArrayList<T>) itemTask.execute(getSubItemParams()).get();
				return mSubItems;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		for (OnCompleteListener<List<T>> listener : itemTask.getOnCompleteListeners()) listener.onComplete(mSubItems);
		return mSubItems;
	}
	
	public void getSubItemsAsync() {
		JrDataTask<List<T>> itemTask = getNewSubItemsTask();
		
		if (mSubItems == null) {
			itemTask.addOnCompleteListener(new OnCompleteListener<List<T>>() {

				@Override
				public void onComplete(List<T> result) {
					mSubItems = (ArrayList<T>) result;
				}
				
			});
			
			itemTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getSubItemParams());
			return;
		}
		
		for (OnCompleteListener<List<T>> listener : itemTask.getOnCompleteListeners()) listener.onComplete(mSubItems);
	}
	
	protected JrDataTask<List<T>> getNewSubItemsTask() {
		JrDataTask<List<T>> subItemsTask = new JrDataTask<List<T>>();

		if (getOnItemsCompleteListeners() != null) {
			for (OnCompleteListener<List<T>> listener : getOnItemsCompleteListeners()) subItemsTask.addOnCompleteListener(listener);
		}
			
		if (getOnItemsStartListeners() != null) {
			for (OnStartListener listener : getOnItemsStartListeners()) subItemsTask.addOnStartListener(listener);
		}
		
		subItemsTask.addOnConnectListener(getOnItemConnectListener());
		
		if (getOnItemsErrorListeners() != null) {
			for (IJrDataTask.OnErrorListener listener : getOnItemsErrorListeners()) subItemsTask.addOnErrorListener(listener);
		}
		
		return subItemsTask;
	}
}
