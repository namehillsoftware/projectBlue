package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.DataTask;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;


public class Files implements IItemFiles {
	private String[] mParams;
	private ArrayList<OnStartListener<List<File>>> mFileStartListeners = new ArrayList<IDataTask.OnStartListener<List<File>>>(1);
	private ArrayList<OnErrorListener<List<File>>> mFileErrorListeners = new ArrayList<IDataTask.OnErrorListener<List<File>>>(1);
	private ArrayList<IDataTask.OnCompleteListener<List<File>>> mFileCompleteListeners;
	public static final int GET_SHUFFLED = 1;

	private OnConnectListener<List<File>> mFileConnectListener = new OnConnectListener<List<File>>() {
		
		@Override
		public List<File> onConnect(InputStream is) {
			ArrayList<File> files = new ArrayList<File>();
			try {
				files = deserializeFileStringList(IOUtils.toString(is));				
			} catch (IOException e) {
				LoggerFactory.getLogger(Files.class).error(e.toString(), e);
			}
			return files;
		}
	};
	
	private OnCompleteListener<List<File>> mFileCompleteListener = new OnCompleteListener<List<File>>() {
		
		@Override
		public void onComplete(ISimpleTask<String, Void, List<File>> owner, List<File> result) {
			
		}
	};
	
	public Files(String... fileParams) {
		mParams = new String[fileParams.length + 1];
		System.arraycopy(fileParams, 0, mParams, 0, fileParams.length);
		mParams[fileParams.length] = "Action=Serialize";
		mFileCompleteListeners = new ArrayList<OnCompleteListener<List<File>>>(2);
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

	public void setOnFilesCompleteListener(OnCompleteListener<List<File>> listener) {
		if (mFileCompleteListeners.size() < 2) mFileCompleteListeners.add(listener);
		else mFileCompleteListeners.set(1, listener);
	}

	public void setOnFilesStartListener(OnStartListener<List<File>> listener) {
		if (mFileStartListeners.size() < 1) mFileStartListeners.add(listener);
		mFileStartListeners.set(0, listener);
	}

	public void setOnFilesErrorListener(OnErrorListener<List<File>> listener) {
		if (mFileErrorListeners.size() < 1) mFileErrorListeners.add(listener);
		mFileErrorListeners.set(0, listener);
	}

	protected OnConnectListener<List<File>> getOnFileConnectListener() {
		return mFileConnectListener;
	}

	protected List<OnCompleteListener<List<File>>> getOnFilesCompleteListeners() {
		return mFileCompleteListeners;
	}

	protected List<OnStartListener<List<File>>> getOnFilesStartListeners() {
		return mFileStartListeners;
	}

	protected List<OnErrorListener<List<File>>> getOnFilesErrorListeners() {
		return mFileErrorListeners;
	}
	
	@Override
	public ArrayList<File> getFiles() {
		try {
			return (ArrayList<File>) getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams()).get();
		} catch (Exception e) {
			LoggerFactory.getLogger(Files.class).error(e.toString(), e);
			return new ArrayList<File>();
		}
	}
	
	public void getFilesAsync() {
		getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams());
	}
	
	@Override
	public ArrayList<File> getFiles(int option) {
		if (option != GET_SHUFFLED) return getFiles();
		
		try {
			return (ArrayList<File>) getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option)).get();
		} catch (Exception e) {
			LoggerFactory.getLogger(Files.class).error(e.toString(), e);
			return getFiles();
		}
	}
	
	public String getFileStringList() throws IOException {
		return getFileStringList(-1);
	}
	
	public String getFileStringList(int option) throws IOException {
		DataTask<String> getStringListTask = new DataTask<String>();
		getStringListTask.addOnConnectListener(new OnConnectListener<String>() {
			
			@Override
			public String onConnect(InputStream is) {
				try {
					return IOUtils.toString(is);
				} catch (IOException e) {
					LoggerFactory.getLogger(Files.class).error(e.toString(), e);
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
			LoggerFactory.getLogger(Files.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(Files.class).error(e.toString(), e);
		}
		
		return result;
	}

	protected DataTask<List<File>> getNewFilesTask() {
		DataTask<List<File>> fileTask = new DataTask<List<File>>();
		
		if (getOnFilesCompleteListeners() != null) {
			for (OnCompleteListener<List<File>> listener : getOnFilesCompleteListeners()) fileTask.addOnCompleteListener(listener);
		}
			
		if (getOnFilesStartListeners() != null) {
			for (OnStartListener<List<File>> listener : getOnFilesStartListeners()) fileTask.addOnStartListener(listener);
		}
		
		fileTask.addOnConnectListener(getOnFileConnectListener());
		
		if (getOnFilesErrorListeners() != null) {
			for (OnErrorListener<List<File>> listener : getOnFilesErrorListeners()) fileTask.addOnErrorListener(listener);
		}
		
		return fileTask;
	}

	public static ArrayList<File> deserializeFileStringList(String fileList) {
		ArrayList<File> files = new ArrayList<File>();
		
		String[] keys = fileList.split(";");
		
		int offset = -1;
		File newFile = null, prevFile = null;
		for (int i = 0; i < keys.length; i++) {
			int intKey = Integer.parseInt(keys[i]);
			if (i == 0) {
				offset = intKey;
				continue;
			}
			if (i == 1) {
				files = new ArrayList<File>(intKey);
				continue;
			}	
			if (i > offset) { 
				newFile = new File(intKey);
				if (prevFile != null)
					prevFile.setNextFile(newFile);

				files.add(newFile);
				prevFile = newFile;
			}
		}
		
		return files;
	}
	
	public static String serializeFileStringList(List<File> files) {
		StringBuilder sb = new StringBuilder("2;");
		sb.append(files.size()).append(";-1;");
		
		for (File file : files)
			sb.append(file.getKey()).append(";");
		
		return sb.toString();
	}
}
