package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.FilesystemResponse;


public class Item extends ItemAsyncBase<Item> implements IItem<Item>, IFilesContainer {
	private ArrayList<OnStartListener<List<Item>>> mItemStartListeners = new ArrayList<OnStartListener<List<Item>>>(1);
	private ArrayList<OnErrorListener<List<Item>>> mItemErrorListeners = new ArrayList<OnErrorListener<List<Item>>>(1);
	private OnCompleteListener<List<Item>> mItemCompleteListener;
	private Files mJrFiles;
	
	private OnConnectListener<List<Item>> mItemConnectListener = new OnConnectListener<List<Item>>() {
		
		@Override
		public List<Item> onConnect(InputStream is) {
			return FilesystemResponse.GetItems(is);
		}
	};
	
	public Item(int key, String value) {
		super(key, value);
	}
	
	public Item(int key) {
		super();
		this.setKey(key);
	}
	
	public Item() {
		super();
	}
	
//	@Override
//	public ArrayList<Item> getSubItems() {
//		if (mSubItems != null && mSubItems.size() > 0) return mSubItems;
//		
//		mSubItems = new ArrayList<Item>();
//		if (JrSession.accessDao == null) return mSubItems;
//		try {
//			List<Item> tempSubItems = getNewSubItemsTask().execute(getSubItemParams()).get();
//			mSubItems.addAll(tempSubItems);
//		} catch (Exception e) {
//			LoggerFactory.getLogger(Item.class).error(e.toString(), e);
//		}
//		
//		return mSubItems;
//	}
	
	@Override
	public IItemFiles getFiles() {
		if (mJrFiles == null) mJrFiles = new Files("Browse/Files", "ID=" + String.valueOf(this.getKey()));
		return mJrFiles;
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<Item>> listener) {
		mItemCompleteListener = listener;
	}

	@Override
	public void setOnItemsStartListener(OnStartListener<List<Item>> listener) {
		if (mItemStartListeners.size() < 1) mItemStartListeners.add(listener); 
		mItemStartListeners.set(0, listener);
	}
	
	@Override
	public void setOnItemsErrorListener(OnErrorListener<List<Item>> listener) {
		if (mItemErrorListeners.size() < 1) mItemErrorListeners.add(listener);
		mItemErrorListeners.set(0, listener);
	}

	@Override
	protected OnConnectListener<List<Item>> getOnItemConnectListener() {
		return mItemConnectListener;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<OnCompleteListener<List<Item>>> getOnItemsCompleteListeners() {
		return Arrays.asList(mItemCompleteListener);
	}

	@Override
	protected List<OnStartListener<List<Item>>> getOnItemsStartListeners() {
		return mItemStartListeners;
	}

	@Override
	protected List<OnErrorListener<List<Item>>> getOnItemsErrorListeners() {
		return mItemErrorListeners;
	}

	@Override
	protected String[] getSubItemParams() {
		return new String[] { "Browse/Children", "ID=" + String.valueOf(this.getKey())};
	}
}
