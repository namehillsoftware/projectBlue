package jrFileSystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jrAccess.JrFsResponse;
import jrAccess.JrSession;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnErrorListener;
import jrFileSystem.IJrDataTask.OnStartListener;

public class JrItem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem>, IJrFilesContainer {
	private ArrayList<JrItem> mSubItems;
	private OnStartListener mItemStartListener;
	private ArrayList<OnCompleteListener<List<JrItem>>> mItemCompleteListeners;
	private OnErrorListener mItemErrorListener;
	private JrFiles mFilesConnector;	
	private OnConnectListener<List<JrItem>> mItemConnectListener = new OnConnectListener<List<JrItem>>() {
		
		@Override
		public List<JrItem> onConnect(InputStream is) {
			return JrFsResponse.GetItems(is);
		}
	};
		
	private OnCompleteListener<List<JrItem>> mItemCompleteListener = new OnCompleteListener<List<JrItem>>() {
		
		@Override
		public void onComplete(List<JrItem> result) {
			mSubItems = (ArrayList<JrItem>) result;
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
	
	@Override
	public ArrayList<JrItem> getSubItems() {
		if (mSubItems != null) return mSubItems;
		
		mSubItems = new ArrayList<JrItem>();
		if (JrSession.accessDao == null) return mSubItems;
		try {
			List<JrItem> tempSubItems = getNewSubItemsTask().execute("Browse/Children", "ID=" + String.valueOf(this.getKey())).get();
			mSubItems.addAll(tempSubItems);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mSubItems;
	}
	
	
//
//	@Override
//	public ArrayList<JrFile> getFiles(int option) {
//		ArrayList<JrFile> returnFiles = new ArrayList<JrFile>();
//		returnFiles.addAll(getFiles());
//		if (option == GET_SHUFFLED) Collections.shuffle(returnFiles, new Random(new Date().getTime()));
//		
//		return returnFiles;
//	}
	
	@Override
	public IJrItemFiles getJrFiles() {
		if (mFilesConnector == null) mFilesConnector = new JrFiles("Browse/Files", "ID=" + String.valueOf(this.getKey()), "Fields=Key,Name");
		return mFilesConnector;
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrItem>> listener) {
		if (mItemCompleteListeners == null) {
			mItemCompleteListeners = new ArrayList<OnCompleteListener<List<JrItem>>>(2);
			mItemCompleteListeners.add(mItemCompleteListener);
		}
		if (mItemCompleteListeners.size() < 2) mItemCompleteListeners.add(listener);
		else mItemCompleteListeners.set(1, listener);
	}

	@Override
	public void setOnItemsStartListener(OnStartListener listener) {
		mItemStartListener = listener;		
	}
	
	@Override
	public void setOnItemsErrorListener(OnErrorListener listener) {
		mItemErrorListener = listener;
	}

	@Override
	protected OnConnectListener<List<JrItem>> getOnItemConnectListener() {
		return mItemConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<JrItem>>> getOnItemsCompleteListeners() {
		return mItemCompleteListeners;
	}

	@Override
	protected List<OnStartListener> getOnItemsStartListeners() {
		ArrayList<OnStartListener> listeners = new ArrayList<IJrDataTask.OnStartListener>(1);
		listeners.add(mItemStartListener);
		return listeners;
	}

	@Override
	protected List<OnErrorListener> getOnItemsErrorListeners() {
		ArrayList<OnErrorListener> listeners = new ArrayList<OnErrorListener>(1);
		listeners.add(mItemErrorListener);
		return listeners;
	}

	@Override
	protected String[] getSubItemParams() {
		return new String[] { "Browse/Children", "ID=" + String.valueOf(this.getKey())};
	}
}
