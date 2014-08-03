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
	private final String[] mParams;
	private OnStartListener<List<File>> mFileStartListener;
	private OnErrorListener<List<File>> mFileErrorListener;
	private IDataTask.OnCompleteListener<List<File>> mFileCompleteListener;
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
	
	public Files(String... fileParams) {
		mParams = new String[fileParams.length + 1];
		System.arraycopy(fileParams, 0, mParams, 0, fileParams.length);
		mParams[fileParams.length] = "Action=Serialize";
	}
	
	/* Required Methods for File Async retrieval */
	protected String[] getFileParams() {
		return mParams;
	}
	
	protected String[] getFileParams(final int option) {
		switch (option) {
			case GET_SHUFFLED:
				final String[] fileParams = new String[mParams.length + 1];
				System.arraycopy(mParams, 0, fileParams, 0, mParams.length);
				fileParams[mParams.length] = "Shuffle=1";
				return fileParams;
			default:
				return mParams;
		}
	}

	public void setOnFilesCompleteListener(OnCompleteListener<List<File>> listener) {
		mFileCompleteListener = listener;
	}

	public void setOnFilesStartListener(OnStartListener<List<File>> listener) {
		mFileStartListener = listener;
	}

	public void setOnFilesErrorListener(OnErrorListener<List<File>> listener) {
		mFileErrorListener = listener;
	}

	protected OnConnectListener<List<File>> getOnFileConnectListener() {
		return mFileConnectListener;
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
		final DataTask<String> getStringListTask = new DataTask<String>();
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
		final DataTask<List<File>> fileTask = new DataTask<List<File>>();
		
		if (mFileCompleteListener != null)
			fileTask.addOnCompleteListener(mFileCompleteListener);
			
		if (mFileStartListener != null)
			fileTask.addOnStartListener(mFileStartListener);
		
		fileTask.addOnConnectListener(getOnFileConnectListener());
		
		if (mFileErrorListener != null)
			fileTask.addOnErrorListener(mFileErrorListener);
		
		return fileTask;
	}

	public static final ArrayList<File> deserializeFileStringList(String fileList) {
		final String[] keys = fileList.split(";");
		final int keyLength = keys.length;
		
		final int offset = Integer.parseInt(keys[0]) + 1;
		final ArrayList<File> files = new ArrayList<File>(Integer.parseInt(keys[1]));
		
		File prevFile = null;
		for (int i = offset; i < keyLength; i++) {
			if (keys[i].equals("-1")) continue;
			
			final int intKey = Integer.parseInt(keys[i]);
			final File newFile = new File(intKey);
			if (prevFile != null)
				prevFile.setNextFile(newFile);

			files.add(newFile);
			prevFile = newFile;
		}
		
		return files;
	}
	
	public static final String serializeFileStringList(List<File> files) {
		final StringBuilder sb = new StringBuilder("2;");
		sb.append(files.size()).append(";-1;");
		
		for (File file : files)
			sb.append(file.getKey()).append(";");
		
		return sb.toString();
	}
}
