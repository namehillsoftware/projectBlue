package com.lasthopesoftware.bluewater.data.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.lasthopesoftware.bluewater.data.access.JrFsResponse;
import com.lasthopesoftware.bluewater.data.access.JrSession;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.threading.ISimpleTask;

public class JrFileSystem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem> {
	private ArrayList<JrItem> mPages;
	private ArrayList<OnCompleteListener<List<JrItem>>> mOnCompleteListeners;
	private OnStartListener<List<JrItem>> mOnStartListener;
	private OnConnectListener<List<JrItem>> mOnConnectListener;
	private OnErrorListener<List<JrItem>> mOnErrorListener;
	
	public JrFileSystem() {
		super();
		OnCompleteListener<List<JrItem>> completeListener = new OnCompleteListener<List<JrItem>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<JrItem>> owner, List<JrItem> result) {
				mSubItems = new ArrayList<JrItem>(result.size());
				mPages.addAll(result);				
			}
		};
		mOnCompleteListeners = new ArrayList<OnCompleteListener<List<JrItem>>>(2);
		mOnCompleteListeners.add(completeListener);
		
		mOnConnectListener = new OnConnectListener<List<JrItem>>() {
			
			@Override
			public List<JrItem> onConnect(InputStream is) {
				return JrFsResponse.GetItems(is);
			}
		};
		//		setPages();
	}
	
	public String getSubItemUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children");
	}
	
	@Override
	public ArrayList<JrItem> getSubItems() {
		if (mPages == null) {
			mPages = new ArrayList<JrItem>();
			if (JrSession.accessDao == null) return mPages;
			
			List<JrItem> tempItems;
			try {
				tempItems = getNewSubItemsTask().execute(getSubItemParams()).get();
				mPages.addAll(tempItems);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mPages;
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrItem>> listener) {
		if (mOnCompleteListeners.size() < 2) mOnCompleteListeners.add(listener);
		mOnCompleteListeners.set(1, listener);
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

