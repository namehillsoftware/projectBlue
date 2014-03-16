package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.lasthopesoftware.bluewater.data.session.JrSession;

public class JrFile extends JrObject {
	private JrFile mNextFile, mPreviousFile;
	private JrFileProperties mFileProperties;
	
	public JrFile(int key) {
		this();
		this.setKey(key);
	}
	
	public JrFile(int key, String value) {
		this();
		this.setKey(key);
		this.setValue(value);
	}
	
	public JrFile() {
		super();
	}
	
	@Override
	public void setKey(Integer key) {
		super.setKey(key);
		mFileProperties = new JrFileProperties(key);
	}

	/**
	 * @return the Value
	 */
	@Override
	public String getValue() {
		if (super.getValue() == null) {
			try {
				super.setValue(mFileProperties.getProperty(JrFileProperties.NAME));
			} catch (IOException e) {
				LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
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
		return JrSession.accessDao.getJrUrl("File/GetFile", "File=" + Integer.toString(getKey()), "Quality=medium", "Conversion=Android", "Playback=0");
	}
		
	public JrFile getNextFile() {
		return mNextFile;
	}
	
	public void setNextFile(JrFile file) {
		mNextFile = file;
	}
	
	public JrFile getPreviousFile() {
		return mPreviousFile;
	}
	
	public void setPreviousFile(JrFile file) {
		mPreviousFile = file;
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
		String durationToParse = mFileProperties.getProperty(JrFileProperties.DURATION);
		if (durationToParse != null && !durationToParse.isEmpty())
			return (int) (Double.parseDouble(durationToParse) * 1000);
		throw new IOException("Duration was not present in the song properties.");
	}
}
