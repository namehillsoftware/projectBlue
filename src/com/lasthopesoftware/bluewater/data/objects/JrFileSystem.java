package com.lasthopesoftware.bluewater.data.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.access.JrFsResponse;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class JrFileSystem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem> {
	private HashMap<String, JrItem> mViews;
	private int[] mVisibleViewKeys;
	
	private OnCompleteListener<List<JrItem>> mOnCompleteClientListener, mOnCompleteViewsListener;
	private OnStartListener<List<JrItem>> mOnStartListener;
	private OnConnectListener<List<JrItem>> mOnConnectListener;
	private OnErrorListener<List<JrItem>> mOnErrorListener;
	
	public JrFileSystem(int... visibleViewKeys) {
		super();
		mVisibleViewKeys = visibleViewKeys;
		
		mOnConnectListener = new OnConnectListener<List<JrItem>>() {
			
			@Override
			public List<JrItem> onConnect(InputStream is) {
				return (LinkedList<JrItem>) JrFsResponse.GetItems(is);
			}
		};
		//		setPages();
	}
	
	public String getSubItemUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children");
	}
	
	public void getVisibleViewsAsync() {
		getVisibleViewsAsync(null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<JrItem, Void, HashMap<String, JrItem>> onCompleteListener) {
		final ISimpleTask.OnCompleteListener<JrItem, Void, HashMap<String, JrItem>> mOnCompleteListener = onCompleteListener;
		
		mOnCompleteViewsListener = new OnCompleteListener<List<JrItem>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<JrItem>> owner, List<JrItem> result) {
				mViews = new HashMap<String, JrItem>();
				SimpleTask<JrItem, Void, HashMap<String, JrItem>> getViewsTask = new SimpleTask<JrItem, Void, HashMap<String, JrItem>>();
				
				
				getViewsTask.addOnExecuteListener(new OnExecuteListener<JrItem, Void, HashMap<String,JrItem>>() {
					
					@Override
					public void onExecute(ISimpleTask<JrItem, Void, HashMap<String, JrItem>> owner, JrItem... params) throws Exception {
						for (JrItem library : params) {
							if (mVisibleViewKeys.length < 1) {
								for (JrItem view : library.getSubItems())
									mViews.put(view.getValue(), view); 
								continue;
							}
							
							for (int viewKey : mVisibleViewKeys) {
								if (viewKey != library.getKey()) continue;
								
								for (JrItem view : library.getSubItems())
									mViews.put(view.getValue(), view);
							}
						}
					}
				});
				
				if (mOnCompleteListener != null) getViewsTask.addOnCompleteListener(mOnCompleteListener);
				
				JrItem[] libraries = new JrItem[result.size()];
				result.toArray(libraries);
				getViewsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, libraries);
			}
		};
		
		getSubItemsAsync();
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrItem>> listener) {
		mOnCompleteClientListener = listener;
	}

	@Override
	public void setOnItemsStartListener(OnStartListener<List<JrItem>> listener) {
		mOnStartListener = listener;
	}

	@Override
	public void setOnItemsErrorListener(OnErrorListener<List<JrItem>> listener) {
		mOnErrorListener = listener;
	}

	@Override
	protected OnConnectListener<List<JrItem>> getOnItemConnectListener() {
		return mOnConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<JrItem>>> getOnItemsCompleteListeners() {
		LinkedList<OnCompleteListener<List<JrItem>>> listeners = new LinkedList<OnCompleteListener<List<JrItem>>>();
		if (mOnCompleteViewsListener != null) listeners.add(mOnCompleteViewsListener);
		if (mOnCompleteClientListener != null) listeners.add(mOnCompleteClientListener);
		return listeners;
	}

	@Override
	protected List<OnStartListener<List<JrItem>>> getOnItemsStartListeners() {
		LinkedList<OnStartListener<List<JrItem>>> listeners = new LinkedList<OnStartListener<List<JrItem>>>();
		if (mOnStartListener != null) listeners.add(mOnStartListener);
		return listeners;
	}

	@Override
	protected List<OnErrorListener<List<JrItem>>> getOnItemsErrorListeners() {
		LinkedList<OnErrorListener<List<JrItem>>> listeners = new LinkedList<OnErrorListener<List<JrItem>>>();
		if (mOnErrorListener != null) listeners.add(mOnErrorListener);
		return listeners;
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children" };
	}
}

