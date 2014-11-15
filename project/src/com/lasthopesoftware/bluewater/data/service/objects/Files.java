package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.DataTask;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;


public class Files implements IItemFiles {
	private final static Logger mLogger = LoggerFactory.getLogger(Files.class);
	
	private final String[] mParams;
	private OnStartListener<List<File>> mFileStartListener;
	private OnErrorListener<List<File>> mFileErrorListener;
	private IDataTask.OnCompleteListener<List<File>> mFileCompleteListener;
	public static final int GET_SHUFFLED = 1;

	private final OnConnectListener<List<File>> mFileConnectListener = new OnConnectListener<List<File>>() {
		
		@Override
		public List<File> onConnect(InputStream is) {
			ArrayList<File> files = new ArrayList<File>();
			try {
				files = deserializeFileStringList(IOUtils.toString(is));				
			} catch (IOException e) {
				mLogger.error(e.toString(), e);
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
		return getFiles(-1);
	}
	
	public void getFilesAsync() {
		getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams());
	}
	
	@Override
	public ArrayList<File> getFiles(int option) {
		try {
			return (ArrayList<File>) getNewFilesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option)).get();
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
			return getFiles();
		}
	}
	
	public final void getFileStringList(OnCompleteListener< String> onGetStringListComplete) {
		getFileStringList(-1, onGetStringListComplete);
	}
	
	public final void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete) {
		getFileStringList(option, onGetStringListComplete, null);
	}
	
	public void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete, final IDataTask.OnErrorListener<String> onGetStringListError) {
		final DataTask<String> getStringListTask = new DataTask<String>(new OnConnectListener<String>() {
			
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
		
		if (onGetStringListError != null)
			getStringListTask.addOnErrorListener(onGetStringListError);
		
		getStringListTask.addOnCompleteListener(onGetStringListComplete);
		getStringListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option));
	}

	protected DataTask<List<File>> getNewFilesTask() {
		final DataTask<List<File>> fileTask = new DataTask<List<File>>(getOnFileConnectListener());
		
		if (mFileCompleteListener != null)
			fileTask.addOnCompleteListener(mFileCompleteListener);
			
		if (mFileStartListener != null)
			fileTask.addOnStartListener(mFileStartListener);
		
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
