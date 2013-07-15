package jrFileSystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jrAccess.JrFsResponse;
import jrAccess.JrSession;
import jrAccess.JrStdXmlResponse;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnErrorListener;
import jrFileSystem.IJrDataTask.OnStartListener;

public class JrFileSystem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem> {
	private ArrayList<JrItem> mPages;
	private ArrayList<OnCompleteListener<List<JrItem>>> mOnCompleteListeners;
	private OnStartListener mOnStartListener;
	private OnConnectListener<List<JrItem>> mOnConnectListener;
	private OnErrorListener mOnErrorListener;
	
	public JrFileSystem() {
		super();
		OnCompleteListener<List<JrItem>> completeListener = new OnCompleteListener<List<JrItem>>() {
			
			@Override
			public void onComplete(List<JrItem> result) {
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
	public void setOnItemsStartListener(OnStartListener listener) {
		mOnStartListener = listener;
	}

	@Override
	public void setOnItemsErrorListener(OnErrorListener listener) {
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
	protected List<OnStartListener> getOnItemsStartListeners() {
		LinkedList<OnStartListener> listeners = new LinkedList<IJrDataTask.OnStartListener>();
		if (mOnStartListener != null) listeners.add(mOnStartListener);
		return listeners;
	}

	@Override
	protected List<OnErrorListener> getOnItemsErrorListeners() {
		LinkedList<OnErrorListener> listeners = new LinkedList<IJrDataTask.OnErrorListener>();
		if (mOnErrorListener != null) listeners.add(mOnErrorListener);
		return listeners;
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children" };
	}
}

