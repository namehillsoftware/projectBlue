package jrFileSystem;

import java.io.InputStream;
import java.lang.reflect.Array;
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
	private ArrayList<OnStartListener> mFileStartListeners = new ArrayList<IJrDataTask.OnStartListener>(1);
	private ArrayList<OnErrorListener> mFileErrorListeners = new ArrayList<IJrDataTask.OnErrorListener>(1);
	private ArrayList<OnCompleteListener<List<JrFile>>> mFileCompleteListeners;
	public static int GET_SHUFFLED = 1;

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
		if (mFileStartListeners.size() < 1) mFileStartListeners.add(listener);
		mFileStartListeners.set(0, listener);
	}

	public void setOnFilesErrorListener(OnErrorListener listener) {
		if (mFileErrorListeners.size() < 1) mFileErrorListeners.add(listener);
		mFileErrorListeners.set(0, listener);
	}

	protected OnConnectListener<List<JrFile>> getOnFileConnectListener() {
		return mFileConnectListener;
	}

	protected List<OnCompleteListener<List<JrFile>>> getOnFilesCompleteListeners() {
		return mFileCompleteListeners;
	}

	protected List<OnStartListener> getOnFilesStartListeners() {
		return mFileStartListeners;
	}

	protected List<OnErrorListener> getOnFilesErrorListeners() {
		return mFileErrorListeners;
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
		if (option != GET_SHUFFLED) return getFiles();
		
		mFiles = new ArrayList<JrFile>();
		try {
			String[] fileParams = new String[getFileParams().length + 1];
			System.arraycopy(getFileParams(), 0, fileParams, 0, getFileParams().length);
			fileParams[fileParams.length - 1] = "Shuffle=1";
			List<JrFile> tempFiles = getNewFilesTask().execute(fileParams).get(); 
			for (int i = 0; i < tempFiles.size(); i++) {
				JrFileUtils.SetSiblings(i, tempFiles);
				mFiles.add(tempFiles.get(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mFiles;
	}
}
