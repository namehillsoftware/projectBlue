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

public class JrFileSystem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem> {
	private HashMap<String, JrItem> mViews;
	private int[] mVisibleViewKeys;
	
	private ArrayList<OnCompleteListener<List<JrItem>>> mOnCompleteListeners;
	private OnStartListener<List<JrItem>> mOnStartListener;
	private OnConnectListener<List<JrItem>> mOnConnectListener;
	private OnErrorListener<List<JrItem>> mOnErrorListener;
	
	public JrFileSystem(int... visibleViewKeys) {
		super();
		mVisibleViewKeys = visibleViewKeys;
		
		mOnCompleteListeners = new ArrayList<OnCompleteListener<List<JrItem>>>(1);
		
		mOnConnectListener = new OnConnectListener<List<JrItem>>() {
			
			@Override
			public List<JrItem> onConnect(InputStream is) {
				LinkedList<JrItem> libraries = (LinkedList<JrItem>) JrFsResponse.GetItems(is);
				
//				mViews = new HashMap<String, JrItem>();
//				for (JrItem library : libraries) {
//					if (mVisibleViewKeys.length < 1) {
//						for (JrItem view : library.getSubItems())
//						mViews.put(view.getValue(), view);
//						continue;
//					}
//					
//					for (int viewKey : mVisibleViewKeys) {
//						if (viewKey == view.getKey())
//							mViews.put(view.getValue(), view);
//					}
//				}
				
				return libraries;
			}
		};
		//		setPages();
	}
	
	public String getSubItemUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children");
	}
	
	public void getVisibleViewsAsync() {
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrItem>> listener) {
		if (mOnCompleteListeners.size() < 1) mOnCompleteListeners.add(listener);
		mOnCompleteListeners.set(0, listener);
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
		return mOnCompleteListeners;
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

