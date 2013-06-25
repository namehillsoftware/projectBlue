package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;

import jrAccess.JrDataTask;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnStartListener;

public abstract class JrItemAsyncBase<T extends JrObject> extends JrObject implements IJrItem<T>, IJrItemFiles {
	protected ArrayList<T> mSubItems;
	
	public JrItemAsyncBase(int key, String value) {
		super(key, value);
	}
	
	public JrItemAsyncBase(String value) {
		super(value);
	}
	
	public JrItemAsyncBase() {
	}
	
	/* Required Methods for Sub Item Async retrieval */
	protected abstract String[] getSubItemParams();
	public abstract void setOnItemsCompleteListener(IJrDataTask.OnCompleteListener<List<T>> listener);
	public abstract void setOnItemsStartListener(IJrDataTask.OnStartListener listener);
	public abstract void setOnItemsErrorListener(IJrDataTask.OnErrorListener listener);
	protected abstract OnConnectListener<List<T>> getOnItemConnectListener();
	protected abstract List<IJrDataTask.OnCompleteListener<List<T>>> getOnItemsCompleteListeners();
	protected abstract List<IJrDataTask.OnStartListener> getOnItemsStartListeners();
	protected abstract List<IJrDataTask.OnErrorListener> getOnItemsErrorListeners();
	
	/* Required Methods for File Async retrieval */
	protected abstract String[] getFileParams();
	public abstract void setOnFilesCompleteListener(IJrDataTask.OnCompleteListener<List<JrFile>> listener);
	public abstract void setOnFilesStartListener(IJrDataTask.OnStartListener listener);
	public abstract void setOnFilesErrorListener(IJrDataTask.OnErrorListener listener);
	protected abstract OnConnectListener<List<JrFile>> getOnFileConnectListener();
	protected abstract List<IJrDataTask.OnCompleteListener<List<JrFile>>> getOnFilesCompleteListeners();
	protected abstract List<IJrDataTask.OnStartListener> getOnFilesStartListeners();
	protected abstract List<IJrDataTask.OnErrorListener> getOnFilesErrorListeners();
	
	public ArrayList<T> getSubItems() {
		
		if (mSubItems != null) {
			try {
				getNewSubItemsTask().execute(getSubItemParams()).get();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSubItems;
	}
	
	public void getSubItemsAsync() {
		JrDataTask<List<T>> itemTask = getNewSubItemsTask();
		
		if (mSubItems != null) {
			itemTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getSubItemParams());
			return;
		}
		
		for (OnCompleteListener<List<T>> listener : itemTask.getOnCompleteListeners()) listener.onComplete(mSubItems);
	}
	
	public void getFilesAsync() {
		getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams());
	}

	protected JrDataTask<List<T>> getNewSubItemsTask() {
		JrDataTask<List<T>> subItemsTask = new JrDataTask<List<T>>();

		if (getOnItemsCompleteListeners() != null) {
			for (OnCompleteListener<List<T>> listener : getOnItemsCompleteListeners()) subItemsTask.addOnCompleteListener(listener);
		}
			
		if (getOnItemsStartListeners() != null) {
			for (OnStartListener listener : getOnItemsStartListeners()) subItemsTask.addOnStartListener(listener);
		}
		
		subItemsTask.addOnConnectListener(getOnItemConnectListener());
		
		if (getOnItemsErrorListeners() != null) {
			for (IJrDataTask.OnErrorListener listener : getOnItemsErrorListeners()) subItemsTask.addOnErrorListener(listener);
		}
		
		return subItemsTask;
	}
	
	
	protected JrDataTask<List<JrFile>> getNewFilesTask() {
		JrDataTask<List<JrFile>> fileTask = new JrDataTask<List<JrFile>>();
		
		if (getOnItemsCompleteListeners() != null) {
			for (OnCompleteListener<List<JrFile>> listener : getOnFilesCompleteListeners()) fileTask.addOnCompleteListener(listener);
		}
			
		if (getOnItemsStartListeners() != null) {
			for (OnStartListener listener : getOnFilesStartListeners()) fileTask.addOnStartListener(listener);
		}
		
		fileTask.addOnConnectListener(getOnFileConnectListener());
		
		if (getOnItemsErrorListeners() != null) {
			for (IJrDataTask.OnErrorListener listener : getOnFilesErrorListeners()) fileTask.addOnErrorListener(listener);
		}
		
		return fileTask;
	}
}
