package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.JrFsResponse;


public class JrItem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem>, IJrFilesContainer {
	private ArrayList<OnStartListener<List<JrItem>>> mItemStartListeners = new ArrayList<OnStartListener<List<JrItem>>>(1);
	private ArrayList<OnErrorListener<List<JrItem>>> mItemErrorListeners = new ArrayList<OnErrorListener<List<JrItem>>>(1);
	private OnCompleteListener<List<JrItem>> mItemCompleteListener;
	private JrFiles mJrFiles;
	
	private OnConnectListener<List<JrItem>> mItemConnectListener = new OnConnectListener<List<JrItem>>() {
		
		@Override
		public List<JrItem> onConnect(InputStream is) {
			return JrFsResponse.GetItems(is);
		}
	};
	
	public JrItem(int key, String value) {
		super(key, value);
	}
	
	public JrItem(int key) {
		super();
		this.setKey(key);
	}
	
	public JrItem() {
		super();
	}
	
//	@Override
//	public ArrayList<JrItem> getSubItems() {
//		if (mSubItems != null && mSubItems.size() > 0) return mSubItems;
//		
//		mSubItems = new ArrayList<JrItem>();
//		if (JrSession.accessDao == null) return mSubItems;
//		try {
//			List<JrItem> tempSubItems = getNewSubItemsTask().execute(getSubItemParams()).get();
//			mSubItems.addAll(tempSubItems);
//		} catch (Exception e) {
//			LoggerFactory.getLogger(JrItem.class).error(e.toString(), e);
//		}
//		
//		return mSubItems;
//	}
	
	@Override
	public IJrItemFiles getJrFiles() {
		if (mJrFiles == null) mJrFiles = new JrFiles("Browse/Files", "ID=" + String.valueOf(this.getKey()));
		return mJrFiles;
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrItem>> listener) {
		mItemCompleteListener = listener;
	}

	@Override
	public void setOnItemsStartListener(OnStartListener<List<JrItem>> listener) {
		if (mItemStartListeners.size() < 1) mItemStartListeners.add(listener); 
		mItemStartListeners.set(0, listener);
	}
	
	@Override
	public void setOnItemsErrorListener(OnErrorListener<List<JrItem>> listener) {
		if (mItemErrorListeners.size() < 1) mItemErrorListeners.add(listener);
		mItemErrorListeners.set(0, listener);
	}

	@Override
	protected OnConnectListener<List<JrItem>> getOnItemConnectListener() {
		return mItemConnectListener;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<OnCompleteListener<List<JrItem>>> getOnItemsCompleteListeners() {
		return Arrays.asList(mItemCompleteListener);
	}

	@Override
	protected List<OnStartListener<List<JrItem>>> getOnItemsStartListeners() {
		return mItemStartListeners;
	}

	@Override
	protected List<OnErrorListener<List<JrItem>>> getOnItemsErrorListeners() {
		return mItemErrorListeners;
	}

	@Override
	protected String[] getSubItemParams() {
		return new String[] { "Browse/Children", "ID=" + String.valueOf(this.getKey())};
	}
}
