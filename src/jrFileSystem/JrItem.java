package jrFileSystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import jrAccess.JrFileXmlResponse;
import jrAccess.JrFsResponse;
import jrAccess.JrSession;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnErrorListener;
import jrFileSystem.IJrDataTask.OnStartListener;
import android.os.AsyncTask;

public class JrItem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem>, IJrItemFiles {
	private ArrayList<JrItem> mSubItems;
	private ArrayList<JrFile> mFiles;
	private OnStartListener mItemStartListener, mFileStartListener;
	private ArrayList<OnCompleteListener<List<JrItem>>> mItemCompleteListeners;
	private ArrayList<OnCompleteListener<List<JrFile>>> mFileCompleteListeners;
	private OnErrorListener mItemErrorListener, mFileErrorListener;
	
	private OnConnectListener<List<JrItem>> mItemConnectListener = new OnConnectListener<List<JrItem>>() {
		
		@Override
		public List<JrItem> onConnect(InputStream is) {
			return JrFsResponse.GetItems(is);
		}
	};
	
	private OnConnectListener<List<JrFile>> mFileConnectListener = new OnConnectListener<List<JrFile>>() {
		
		@Override
		public List<JrFile> onConnect(InputStream is) {
			return JrFileXmlResponse.GetFiles(is);
		}
	};
	
	private OnCompleteListener<List<JrItem>> mItemCompleteListener = new OnCompleteListener<List<JrItem>>() {
		
		@Override
		public void onComplete(List<JrItem> result) {
			mSubItems = (ArrayList<JrItem>) result;
		}
	};
	
	private OnCompleteListener<List<JrFile>> mFileCompleteListener = new OnCompleteListener<List<JrFile>>() {
		
		@Override
		public void onComplete(List<JrFile> result) {
			mFiles = (ArrayList<JrFile>)result;
//			if (option == GET_SHUFFLED) Collections.shuffle(returnFiles, new Random(new Date().getTime()));
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
	
	@Override
	public ArrayList<JrFile> getFiles() {
		if (mFiles != null) return mFiles;
		
		mFiles = new ArrayList<JrFile>();
		try {
			List<JrFile> tempFiles = getNewFilesTask().execute("Browse/Files", "ID=" + String.valueOf(this.getKey()), "Fields=Key,Name").get(); 
			for (int i = 0; i < tempFiles.size(); i++) {
				JrFileUtils.SetSiblings(i, tempFiles);
				mFiles.add(tempFiles.get(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mFiles;
	}

	@Override
	public ArrayList<JrFile> getFiles(int option) {
		ArrayList<JrFile> returnFiles = new ArrayList<JrFile>();
		returnFiles.addAll(getFiles());
		if (option == GET_SHUFFLED) Collections.shuffle(returnFiles, new Random(new Date().getTime()));
		
		return returnFiles;
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
	public void getFilesAsync() {
		getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams());
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
	public void setOnFilesCompleteListener(OnCompleteListener<List<JrFile>> listener) {
		if (mFileCompleteListeners == null) {
			mFileCompleteListeners = new ArrayList<OnCompleteListener<List<JrFile>>>(2);
			mFileCompleteListeners.add(mFileCompleteListener);
		}
		if (mFileCompleteListeners.size() < 2) mFileCompleteListeners.add(listener);
		else mFileCompleteListeners.set(1, listener);
	}

	@Override
	public void setOnFilesStartListener(OnStartListener listener) {
		mFileStartListener = listener;
	}

	@Override
	public void setOnFilesErrorListener(OnErrorListener listener) {
		mFileErrorListener = listener;
	}

	@Override
	protected OnConnectListener<List<JrFile>> getOnFileConnectListener() {
		return mFileConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<JrFile>>> getOnFilesCompleteListeners() {
		return mFileCompleteListeners;
	}

	@Override
	protected List<OnStartListener> getOnFilesStartListeners() {
		ArrayList<OnStartListener> listeners = new ArrayList<OnStartListener>();
		listeners.add(mFileStartListener);
		return listeners;
	}

	@Override
	protected List<OnErrorListener> getOnFilesErrorListeners() {
		ArrayList<OnErrorListener> listeners = new ArrayList<OnErrorListener>();
		listeners.add(mFileErrorListener);
		return listeners;
	}

	@Override
	protected String[] getSubItemParams() {
		return new String[] { "Browse/Children", "ID=" + String.valueOf(this.getKey())};
	}

	@Override
	protected String[] getFileParams() {
		return new String[] { "Browse/Files", "ID=" + String.valueOf(this.getKey()), "Fields=Key,Name" };
	}
}
