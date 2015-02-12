package com.lasthopesoftware.bluewater.data.service.objects;



public class Item extends ItemAsyncBase implements IItem, IFilesContainer {
	private Files mFiles;

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
		
	@Override
	public IItemFiles getFiles() {
		if (mFiles == null) mFiles = new Files("Browse/Files", "ID=" + String.valueOf(this.getKey()));
		return mFiles;
	}
//
//	@Override
//	public void addOnItemsCompleteListener(OnCompleteListener<List<Item>> listener) {
//		if (mOnCompleteListeners == null) mOnCompleteListeners = new ArrayList<OnCompleteListener<List<Item>>>();
//		
//		mOnCompleteListeners.add(listener);
//	}
//
//	@Override
//	public void removeOnItemsCompleteListener(OnCompleteListener<List<Item>> listener) {
//		if (mOnCompleteListeners != null)
//			mOnCompleteListeners.remove(listener);
//	}
//
//	@Override
//	public void setOnItemsStartListener(OnStartListener<List<Item>> listener) {
//		if (mItemStartListeners.size() < 1) mItemStartListeners.add(listener); 
//		mItemStartListeners.set(0, listener);
//	}
//	
//	@Override
//	public void setOnItemsErrorListener(OnErrorListener<List<Item>> listener) {
//		if (mItemErrorListeners.size() < 1) mItemErrorListeners.add(listener);
//		mItemErrorListeners.set(0, listener);
//	}
//
//	@Override
//	protected OnConnectListener<List<Item>> getOnItemConnectListener() {
//		return mItemConnectListener;
//	}
//
//	@Override
//	protected List<OnCompleteListener<List<Item>>> getOnItemsCompleteListeners() {
//		return mOnCompleteListeners;
//	}
//
//	@Override
//	protected List<OnStartListener<List<Item>>> getOnItemsStartListeners() {
//		return mItemStartListeners;
//	}
//
//	@Override
//	protected List<OnErrorListener<List<Item>>> getOnItemsErrorListeners() {
//		return mItemErrorListeners;
//	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children", "ID=" + String.valueOf(this.getKey())};
	}
}
