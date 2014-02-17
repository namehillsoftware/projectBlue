package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.JrDataTask;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;


public class JrFiles implements IJrItemFiles {
	private String[] mParams;
	private ArrayList<OnStartListener<List<JrFile>>> mFileStartListeners = new ArrayList<IJrDataTask.OnStartListener<List<JrFile>>>(1);
	private ArrayList<OnErrorListener<List<JrFile>>> mFileErrorListeners = new ArrayList<IJrDataTask.OnErrorListener<List<JrFile>>>(1);
	private ArrayList<IJrDataTask.OnCompleteListener<List<JrFile>>> mFileCompleteListeners;
	public static final int GET_SHUFFLED = 1;

	private OnConnectListener<List<JrFile>> mFileConnectListener = new OnConnectListener<List<JrFile>>() {
		
		@Override
		public List<JrFile> onConnect(InputStream is) {
			ArrayList<JrFile> files = new ArrayList<JrFile>();
			try {
				files = deserializeFileStringList(IOUtils.toString(is));				
			} catch (IOException e) {
				LoggerFactory.getLogger(JrFiles.class).error(e.toString(), e);
			}
			return files;
		}
	};
	
	private OnCompleteListener<List<JrFile>> mFileCompleteListener = new OnCompleteListener<List<JrFile>>() {
		
		@Override
		public void onComplete(ISimpleTask<String, Void, List<JrFile>> owner, List<JrFile> result) {
			
		}
	};
	
	public JrFiles(String... fileParams) {
		mParams = new String[fileParams.length + 1];
		System.arraycopy(fileParams, 0, mParams, 0, fileParams.length);
		mParams[fileParams.length] = "Action=Serialize";
		mFileCompleteListeners = new ArrayList<OnCompleteListener<List<JrFile>>>(2);
		mFileCompleteListeners.add(mFileCompleteListener);
	}
	
	/* Required Methods for File Async retrieval */
	protected String[] getFileParams() {
		return mParams;
	}
	
	protected String[] getFileParams(int option) {
		switch (option) {
			case GET_SHUFFLED:
				String[] fileParams = new String[mParams.length + 1];
				System.arraycopy(mParams, 0, fileParams, 0, mParams.length);
				fileParams[mParams.length] = "Shuffle=1";
				return fileParams;
			default:
				return mParams;
		}
	}

	public void setOnFilesCompleteListener(OnCompleteListener<List<JrFile>> listener) {
		if (mFileCompleteListeners.size() < 2) mFileCompleteListeners.add(listener);
		else mFileCompleteListeners.set(1, listener);
	}

	public void setOnFilesStartListener(OnStartListener<List<JrFile>> listener) {
		if (mFileStartListeners.size() < 1) mFileStartListeners.add(listener);
		mFileStartListeners.set(0, listener);
	}

	public void setOnFilesErrorListener(OnErrorListener<List<JrFile>> listener) {
		if (mFileErrorListeners.size() < 1) mFileErrorListeners.add(listener);
		mFileErrorListeners.set(0, listener);
	}

	protected OnConnectListener<List<JrFile>> getOnFileConnectListener() {
		return mFileConnectListener;
	}

	protected List<OnCompleteListener<List<JrFile>>> getOnFilesCompleteListeners() {
		return mFileCompleteListeners;
	}

	protected List<OnStartListener<List<JrFile>>> getOnFilesStartListeners() {
		return mFileStartListeners;
	}

	protected List<OnErrorListener<List<JrFile>>> getOnFilesErrorListeners() {
		return mFileErrorListeners;
	}
	
	@Override
	public ArrayList<JrFile> getFiles() {
		try {
			return (ArrayList<JrFile>) getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams()).get();
		} catch (Exception e) {
			LoggerFactory.getLogger(JrFiles.class).error(e.toString(), e);
			return new ArrayList<JrFile>();
		}
	}
	
	public void getFilesAsync() {
		getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams());
	}
	
	@Override
	public ArrayList<JrFile> getFiles(int option) {
		if (option != GET_SHUFFLED) return getFiles();
		
		try {
			return (ArrayList<JrFile>) getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option)).get();
		} catch (Exception e) {
			LoggerFactory.getLogger(JrFiles.class).error(e.toString(), e);
			return getFiles();
		}
	}
	
	public String getFileStringList() throws IOException {
		return getFileStringList(-1);
	}
	
	public String getFileStringList(int option) throws IOException {
		JrDataTask<String> getStringListTask = new JrDataTask<String>();
		getStringListTask.addOnConnectListener(new OnConnectListener<String>() {
			
			@Override
			public String onConnect(InputStream is) {
				try {
					return IOUtils.toString(is);
				} catch (IOException e) {
					LoggerFactory.getLogger(JrFiles.class).error(e.toString(), e);
					return null;
				}
			}
		});
		
		getStringListTask.addOnErrorListener(new ISimpleTask.OnErrorListener<String, Void, String>() {
			
			@Override
			public boolean onError(ISimpleTask<String, Void, String> owner, Exception innerException) {
				return innerException instanceof IOException;
			}
		});
		
		String result = null;
		try {
			result = getStringListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option)).get();
			if (getStringListTask.getState() == SimpleTaskState.ERROR) {
				for (Exception exception : getStringListTask.getExceptions()) {
					if (exception instanceof IOException) throw (IOException)exception;
				}
			}
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(JrFiles.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(JrFiles.class).error(e.toString(), e);
		}
		
		return result;
	}

	protected JrDataTask<List<JrFile>> getNewFilesTask() {
		JrDataTask<List<JrFile>> fileTask = new JrDataTask<List<JrFile>>();
		
		if (getOnFilesCompleteListeners() != null) {
			for (OnCompleteListener<List<JrFile>> listener : getOnFilesCompleteListeners()) fileTask.addOnCompleteListener(listener);
		}
			
		if (getOnFilesStartListeners() != null) {
			for (OnStartListener<List<JrFile>> listener : getOnFilesStartListeners()) fileTask.addOnStartListener(listener);
		}
		
		fileTask.addOnConnectListener(getOnFileConnectListener());
		
		if (getOnFilesErrorListeners() != null) {
			for (OnErrorListener<List<JrFile>> listener : getOnFilesErrorListeners()) fileTask.addOnErrorListener(listener);
		}
		
		return fileTask;
	}

	public static ArrayList<JrFile> deserializeFileStringList(String fileList) {
		ArrayList<JrFile> files = new ArrayList<JrFile>();
		
		String[] keys = fileList.split(";");
		
		int offset = -1;
		JrFile newFile = null, prevFile = null;
		for (int i = 0; i < keys.length; i++) {
			int intKey = Integer.parseInt(keys[i]);
			if (i == 0) {
				offset = intKey;
				continue;
			}
			if (i == 1) {
				files = new ArrayList<JrFile>(intKey);
				continue;
			}	
			if (i > offset) { 
				newFile = new JrFile(intKey);
				if (prevFile != null) {
					newFile.setPreviousFile(prevFile);
					prevFile.setNextFile(newFile);
				}
				files.add(newFile);
				prevFile = newFile;
			}
		}
		
		return files;
	}
	
	public static String serializeFileStringList(ArrayList<JrFile> files) {
		StringBuilder sb = new StringBuilder("2;");
		sb.append(files.size()).append(";-1;");
		
		for (JrFile file : files)
			sb.append(file.getKey()).append(";");
		
		return sb.toString();
	}
}
