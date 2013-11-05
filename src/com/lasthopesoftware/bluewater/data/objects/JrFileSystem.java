package com.lasthopesoftware.bluewater.data.objects;

import java.io.InputStream;
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
	
	private OnCompleteListener<List<JrItem>> mOnCompleteClientListener;
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
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, HashMap<String, JrItem>> onCompleteListener) {
		SimpleTask<String, Void, HashMap<String, JrItem>> getViewsTask = new SimpleTask<String, Void, HashMap<String, JrItem>>();
		
		
		getViewsTask.addOnExecuteListener(new OnExecuteListener<String, Void, HashMap<String,JrItem>>() {
			
			@Override
			public void onExecute(ISimpleTask<String, Void, HashMap<String, JrItem>> owner, String... params) throws Exception {
				List<JrItem> libraries = getSubItems();
				for (JrItem library : libraries) {
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
		
		if (onCompleteListener != null) getViewsTask.addOnCompleteListener(onCompleteListener);
		getViewsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

