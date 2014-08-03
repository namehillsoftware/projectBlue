package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.lasthopesoftware.bluewater.data.service.access.FileProperties;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;

public class File extends BaseObject {
	private File mNextFile, mPreviousFile;
	private FileProperties mFileProperties;
	
	public File(int key) {
		this();
		this.setKey(key);
	}
	
	public File(int key, String value) {
		this();
		this.setKey(key);
		this.setValue(value);
	}
	
	public File() {
		super();
	}
	
	@Override
	public void setKey(int key) {
		super.setKey(key);
		mFileProperties = new FileProperties(key);
	}

	/**
	 * @return the Value
	 */
	@Override
	public String getValue() {
		if (super.getValue() == null) {
			try {
				super.setValue(mFileProperties.getProperty(FileProperties.NAME));
			} catch (IOException e) {
				LoggerFactory.getLogger(File.class).error(e.toString(), e);
			}
		}
		return super.getValue();
	}
	
	public String getSubItemUrl() {
		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.; 
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */
		return ConnectionManager.getFormattedUrl("File/GetFile", "File=" + Integer.toString(getKey()), "Quality=medium", "Conversion=Android", "Playback=0");
	}
		
	public File getNextFile() {
		return mNextFile;
	}
	
	public void setNextFile(final File file) {
		if (mNextFile == file) return;
		mNextFile = file;
		file.setPreviousFile(this);
	}
	
	public File getPreviousFile() {
		return mPreviousFile;
	}
	
	public void setPreviousFile(final File file) {
		if (mPreviousFile == file) return;
		mPreviousFile = file;
		file.setNextFile(this);
	}
	
	public void setProperty(String name, String value) {
		mFileProperties.setProperty(name, value);
	}
	
	public String getProperty(String name) throws IOException {
		return mFileProperties.getProperty(name);
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		return mFileProperties.getRefreshedProperty(name);
	}
	
	public int getDuration() throws IOException {
		String durationToParse = mFileProperties.getProperty(FileProperties.DURATION);
		if (durationToParse != null && !durationToParse.isEmpty())
			return (int) (Double.parseDouble(durationToParse) * 1000);
		throw new IOException("Duration was not present in the song properties.");
	}
}
