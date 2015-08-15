package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.threading.DataTask;
import com.lasthopesoftware.threading.IDataTask;
import com.lasthopesoftware.threading.IDataTask.OnCompleteListener;
import com.lasthopesoftware.threading.IDataTask.OnConnectListener;
import com.lasthopesoftware.threading.IDataTask.OnErrorListener;
import com.lasthopesoftware.threading.IDataTask.OnStartListener;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class Files implements IItemFiles {
	private final static Logger logger = LoggerFactory.getLogger(Files.class);
	
	private final String[] params;
	private OnStartListener<List<IFile>> fileStartListener;
	private OnErrorListener<List<IFile>> fileErrorListener;
	private IDataTask.OnCompleteListener<List<IFile>> fileCompleteListener;
	public static final int GET_SHUFFLED = 1;

	private final ConnectionProvider connectionProvider;

	private final OnConnectListener<List<IFile>> mFileConnectListener = new OnConnectListener<List<IFile>>() {
		
		@Override
		public List<IFile> onConnect(InputStream is) {
			try {
				return parseFileStringList(connectionProvider, IOUtils.toString(is));
			} catch (IOException e) {
				logger.error("Error reading string from stream", e);
				return null;
			}
		}
	};
	
	public Files(ConnectionProvider connectionProvider, String... fileParams) {
		params = new String[fileParams.length + 1];
		System.arraycopy(fileParams, 0, params, 0, fileParams.length);
		params[fileParams.length] = "Action=Serialize";
		this.connectionProvider = connectionProvider;
	}
	
	/* Required Methods for File Async retrieval */
	protected String[] getFileParams() {
		return params;
	}
	
	protected String[] getFileParams(final int option) {
		switch (option) {
			case GET_SHUFFLED:
				final String[] fileParams = new String[params.length + 1];
				System.arraycopy(params, 0, fileParams, 0, params.length);
				fileParams[params.length] = "Shuffle=1";
				return fileParams;
			default:
				return params;
		}
	}

	public void setOnFilesCompleteListener(OnCompleteListener<List<IFile>> listener) {
		fileCompleteListener = listener;
	}

	public void setOnFilesStartListener(OnStartListener<List<IFile>> listener) {
		fileStartListener = listener;
	}

	public void setOnFilesErrorListener(OnErrorListener<List<IFile>> listener) {
		fileErrorListener = listener;
	}

	protected OnConnectListener<List<IFile>> getOnFileConnectListener() {
		return mFileConnectListener;
	}
	
	@Override
	public ArrayList<IFile> getFiles() {
		return getFiles(-1);
	}
	
	public void getFilesAsync() {
		getNewFilesTask().execute(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams());
	}
	
	@Override
	public ArrayList<IFile> getFiles(int option) {
		try {
			return (ArrayList<IFile>) getNewFilesTask().execute(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option)).get();
		} catch (Exception e) {
			logger.error(e.toString(), e);
			return getFiles();
		}
	}
	
	public final void getFileStringList(OnCompleteListener< String> onGetStringListComplete) {
		getFileStringList(onGetStringListComplete, null);
	}
	

	@Override
	public void getFileStringList(OnCompleteListener<String> onGetStringListComplete, OnErrorListener<String> onGetStringListError) {
		getFileStringList(-1, onGetStringListComplete, onGetStringListError);
	}
	
	public final void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete) {
		getFileStringList(option, onGetStringListComplete, null);
	}
	
	public void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete, final IDataTask.OnErrorListener<String> onGetStringListError) {
		final DataTask<String> getStringListTask = new DataTask<>(connectionProvider, new OnConnectListener<String>() {
			
			@Override
			public String onConnect(InputStream is) {
				try {
					return IOUtils.toString(is);
				} catch (IOException e) {
					logger.error("Error reading string from stream", e);
					return null;
				}
			}
		});
		
		if (onGetStringListError != null)
			getStringListTask.addOnErrorListener(onGetStringListError);
		
		getStringListTask.addOnCompleteListener(onGetStringListComplete);
		getStringListTask.execute(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option));
	}

	protected DataTask<List<IFile>> getNewFilesTask() {
		final DataTask<List<IFile>> fileTask = new DataTask<>(connectionProvider, getOnFileConnectListener());
		
		if (fileCompleteListener != null)
			fileTask.addOnCompleteListener(fileCompleteListener);
			
		if (fileStartListener != null)
			fileTask.addOnStartListener(fileStartListener);
		
		if (fileErrorListener != null)
			fileTask.addOnErrorListener(fileErrorListener);
		
		return fileTask;
	}

	public static final ArrayList<IFile> parseFileStringList(ConnectionProvider connectionProvider, String fileList) {
		final String[] keys = fileList.split(";");
		
		final int offset = Integer.parseInt(keys[0]) + 1;
		final ArrayList<IFile> files = new ArrayList<>(Integer.parseInt(keys[1]));
		
		for (int i = offset; i < keys.length; i++) {
			if (keys[i].equals("-1")) continue;
			
			files.add(new File(connectionProvider, Integer.parseInt(keys[i])));
		}
		
		return files;
	}
	
	public static final String serializeFileStringList(List<IFile> files) {
		final int fileSize = files.size();
		// Take a guess that most keys will not be greater than 8 characters and add some more
		// for the first characters
		final StringBuilder sb = new StringBuilder(fileSize * 9 + 8);
		sb.append("2;").append(fileSize).append(";-1;");
		
		for (IFile file : files)
			sb.append(file.getKey()).append(";");
		
		return sb.toString();
	}
}
