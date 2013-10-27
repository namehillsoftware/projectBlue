package com.lasthopesoftware.bluewater.data.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.access.JrFsResponse;
import com.lasthopesoftware.threading.ISimpleTask;

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
		if (mOnCompleteViewsListener == null) {
			mOnCompleteViewsListener = new OnCompleteListener<List<JrItem>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<JrItem>> owner, List<JrItem> result) {
					mViews = new HashMap<String, JrItem>();
					OnCompleteListener<List<JrItem>> librariesCompleteListener = new OnCompleteListener<List<JrItem>>() {
						
						@Override
						public void onComplete(ISimpleTask<String, Void, List<JrItem>> owner, List<JrItem> result) {
							for (JrItem view : result)
								mViews.put(view.getValue(), view); 
						}
					};
					
					for (JrItem library : result) {
						if (mVisibleViewKeys.length < 1) {
							library.setOnItemsCompleteListener(librariesCompleteListener);
							continue;
						}
						
						for (int viewKey : mVisibleViewKeys) {
							if (viewKey == library.getKey())
								library.setOnItemsCompleteListener(librariesCompleteListener);
						}
					}
				}
			};
		}
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

