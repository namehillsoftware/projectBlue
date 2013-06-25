package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.JrSession;
import jrAccess.JrStdXmlResponse;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnErrorListener;
import jrFileSystem.IJrDataTask.OnStartListener;

public class JrFileSystem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem> {
	private ArrayList<JrItem> mPages;
	private ArrayList<OnCompleteListener<List<JrItem>>> mOnCompleteListeners;
	
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
				tempItems = JrFileUtils.transformListing(JrItem.class, (new JrStdXmlResponse()).execute("Browse/Children").get().items);
				mPages = new ArrayList<JrItem>(tempItems.size());
				mPages.addAll(tempItems);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mPages;
	}

	@Override
	public ArrayList<JrFile> getFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<JrFile> getFiles(int option) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrItem>> listener) {
		if (mOnCompleteListeners.size() < 2) mOnCompleteListeners.add(listener);
		mOnCompleteListeners.set(1, listener);
	}

	@Override
	public void setOnItemsStartListener(OnStartListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnItemsErrorListener(OnErrorListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected OnConnectListener<List<JrItem>> getOnItemConnectListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OnCompleteListener<List<JrItem>>> getOnItemsCompleteListeners() {
		return mOnCompleteListeners;
	}

	@Override
	protected List<OnStartListener> getOnItemsStartListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OnErrorListener> getOnItemsErrorListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getFilesAsync() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnFilesCompleteListener(
			OnCompleteListener<List<JrFile>> listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnFilesStartListener(OnStartListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnFilesErrorListener(OnErrorListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected OnConnectListener<List<JrFile>> getOnFileConnectListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OnCompleteListener<List<JrFile>>> getOnFilesCompleteListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OnStartListener> getOnFilesStartListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OnErrorListener> getOnFilesErrorListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] getSubItemParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] getFileParams() {
		// TODO Auto-generated method stub
		return null;
	}
}

