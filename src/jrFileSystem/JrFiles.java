package jrFileSystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;

import jrAccess.JrDataTask;
import jrAccess.JrFileXmlResponse;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnErrorListener;
import jrFileSystem.IJrDataTask.OnStartListener;

public class JrFiles implements IJrItemFiles {
	private ArrayList<JrFile> mFiles;
	private String[] mParams;
	private OnStartListener mFileStartListener;
	private ArrayList<OnCompleteListener<List<JrFile>>> mFileCompleteListeners;
	private OnErrorListener mFileErrorListener;

	private OnConnectListener<List<JrFile>> mFileConnectListener = new OnConnectListener<List<JrFile>>() {
		
		@Override
		public List<JrFile> onConnect(InputStream is) {
			return JrFileXmlResponse.GetFiles(is);
		}
	};
	
	private OnCompleteListener<List<JrFile>> mFileCompleteListener = new OnCompleteListener<List<JrFile>>() {
		
		@Override
		public void onComplete(List<JrFile> result) {
			mFiles = (ArrayList<JrFile>)result;
//			if (option == GET_SHUFFLED) Collections.shuffle(returnFiles, new Random(new Date().getTime()));
		}
	};
	
	public JrFiles(String... fileParams) {
		mParams = fileParams;
	}
	
	/* Required Methods for File Async retrieval */
	protected String[] getFileParams() {
		return mParams;
	}

	public void setOnFilesCompleteListener(OnCompleteListener<List<JrFile>> listener) {
		if (mFileCompleteListeners == null) {
			mFileCompleteListeners = new ArrayList<OnCompleteListener<List<JrFile>>>(2);
			mFileCompleteListeners.add(mFileCompleteListener);
		}
		if (mFileCompleteListeners.size() < 2) mFileCompleteListeners.add(listener);
		else mFileCompleteListeners.set(1, listener);
	}

	public void setOnFilesStartListener(OnStartListener listener) {
		mFileStartListener = listener;
	}

	public void setOnFilesErrorListener(OnErrorListener listener) {
		mFileErrorListener = listener;
	}

	protected OnConnectListener<List<JrFile>> getOnFileConnectListener() {
		return mFileConnectListener;
	}

	protected List<OnCompleteListener<List<JrFile>>> getOnFilesCompleteListeners() {
		return mFileCompleteListeners;
	}

	protected List<OnStartListener> getOnFilesStartListeners() {
		ArrayList<OnStartListener> listeners = new ArrayList<OnStartListener>();
		listeners.add(mFileStartListener);
		return listeners;
	}

	protected List<OnErrorListener> getOnFilesErrorListeners() {
		ArrayList<OnErrorListener> listeners = new ArrayList<OnErrorListener>();
		listeners.add(mFileErrorListener);
		return listeners;
	}
	
	@Override
	public ArrayList<JrFile> getFiles() {
		if (mFiles != null) return mFiles;
		
		mFiles = new ArrayList<JrFile>();
		try {
			List<JrFile> tempFiles = getNewFilesTask().execute(getFileParams()).get(); 
			for (int i = 0; i < tempFiles.size(); i++) {
				JrFileUtils.SetSiblings(i, tempFiles);
				mFiles.add(tempFiles.get(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mFiles;
	}
	
	public void getFilesAsync() {
		getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams());
	}

	protected JrDataTask<List<JrFile>> getNewFilesTask() {
		JrDataTask<List<JrFile>> fileTask = new JrDataTask<List<JrFile>>();
		
		if (getOnFilesCompleteListeners() != null) {
			for (OnCompleteListener<List<JrFile>> listener : getOnFilesCompleteListeners()) fileTask.addOnCompleteListener(listener);
		}
			
		if (getOnFilesStartListeners() != null) {
			for (OnStartListener listener : getOnFilesStartListeners()) fileTask.addOnStartListener(listener);
		}
		
		fileTask.addOnConnectListener(getOnFileConnectListener());
		
		if (getOnFilesErrorListeners() != null) {
			for (IJrDataTask.OnErrorListener listener : getOnFilesErrorListeners()) fileTask.addOnErrorListener(listener);
		}
		
		return fileTask;
	}

	@Override
	public ArrayList<JrFile> getFiles(int option) {
		// TODO Auto-generated method stub
		return null;
	}
}
