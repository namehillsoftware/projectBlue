package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.callables.IOneParameterCallable;
import com.lasthopesoftware.threading.DataTask;
import com.lasthopesoftware.threading.ISimpleTask;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class Files implements IItemFiles {
	private final static Logger logger = LoggerFactory.getLogger(Files.class);
	
	private final String[] params;
	private ISimpleTask.OnStartListener<String, Void, List<IFile>> fileStartListener;
	private ISimpleTask.OnErrorListener<String, Void, List<IFile>> fileErrorListener;
	private ISimpleTask.OnCompleteListener<String, Void, List<IFile>> fileCompleteListener;
	public static final int GET_SHUFFLED = 1;

	private final ConnectionProvider connectionProvider;

	private final IOneParameterCallable<InputStream, List<IFile>> mFileConnectListener = new IOneParameterCallable<InputStream, List<IFile>>() {
		
		@Override
		public List<IFile> call(InputStream is) {
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

	public void setOnFilesCompleteListener(ISimpleTask.OnCompleteListener<String, Void, List<IFile>> listener) {
		fileCompleteListener = listener;
	}

	public void setOnFilesStartListener(ISimpleTask.OnStartListener<String, Void, List<IFile>> listener) {
		fileStartListener = listener;
	}

	public void setOnFilesErrorListener(HandleViewIoException listener) {
		fileErrorListener = listener;
	}

	protected IOneParameterCallable<InputStream, List<IFile>> getOnFileConnectListener() {
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
		} catch (ExecutionException | InterruptedException e) {
			logger.error(e.toString(), e);
			return new ArrayList<>();
		}
	}
	
	public final void getFileStringList(ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete) {
		getFileStringList(onGetStringListComplete, null);
	}
	

	@Override
	public void getFileStringList(ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete, ISimpleTask.OnErrorListener<String, Void, String> onGetStringListError) {
		getFileStringList(-1, onGetStringListComplete, onGetStringListError);
	}
	
	public final void getFileStringList(final int option, final ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete) {
		getFileStringList(option, onGetStringListComplete, null);
	}
	
	public void getFileStringList(final int option, final ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete, final ISimpleTask.OnErrorListener<String, Void, String> onGetStringListError) {
		final DataTask<String> getStringListTask = new DataTask<>(connectionProvider, new IOneParameterCallable<InputStream, String>() {
			
			@Override
			public String call(InputStream is) {
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

	protected ISimpleTask<String, Void, List<IFile>> getNewFilesTask() {
		final ISimpleTask<String, Void, List<IFile>> fileTask = new DataTask<>(connectionProvider, getOnFileConnectListener());
		
		if (fileCompleteListener != null)
			fileTask.addOnCompleteListener(fileCompleteListener);
			
		if (fileStartListener != null)
			fileTask.addOnStartListener(fileStartListener);
		
		if (fileErrorListener != null)
			fileTask.addOnErrorListener(fileErrorListener);
		
		return fileTask;
	}

	public static ArrayList<IFile> parseFileStringList(ConnectionProvider connectionProvider, String fileList) {
		final String[] keys = fileList.split(";");
		
		final int offset = Integer.parseInt(keys[0]) + 1;
		final ArrayList<IFile> files = new ArrayList<>(Integer.parseInt(keys[1]));
		
		for (int i = offset; i < keys.length; i++) {
			if (keys[i].equals("-1")) continue;
			
			files.add(new File(connectionProvider, Integer.parseInt(keys[i])));
		}
		
		return files;
	}
	
	public static String serializeFileStringList(List<IFile> files) {
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
