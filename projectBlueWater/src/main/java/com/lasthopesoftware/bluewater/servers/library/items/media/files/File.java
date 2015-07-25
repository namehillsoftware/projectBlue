package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class File extends AbstractIntKeyStringValue implements IFile {
	private FilePropertiesProvider mFilePropertiesProvider;

	private static final Logger mLogger = LoggerFactory.getLogger(File.class);

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
		mFilePropertiesProvider = new FilePropertiesProvider(key);
	}

	/**
	 * @return the Value
	 */
	@Override
	public String getValue() {
		if (super.getValue() == null) {
			try {
				super.setValue(mFilePropertiesProvider.getProperty(FilePropertiesProvider.NAME));
			} catch (IOException e) {
				mLogger.error(e.toString(), e);
			}
		}
		return super.getValue();
	}

	public String[] getPlaybackParams() {
		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.;
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */

		return new String[] { "File/GetFile", "File=" + Integer.toString(getKey()), "Quality=medium", "Conversion=Android", "Playback=0" };
	}
	
	public String getPlaybackUrl() {
		return ConnectionProvider.getFormattedUrl(getPlaybackParams());
	}
	
	public void setProperty(String name, String value) {
		mFilePropertiesProvider.setProperty(name, value);
	}
	
	public String getProperty(String name) throws IOException {
		return mFilePropertiesProvider.getProperty(name);
	}

	@Override
	public String tryGetProperty(String name) {
		try {
			return mFilePropertiesProvider.getProperty(name);
		} catch (IOException io) {
			mLogger.warn("There was an error returning " + name + " for file " + String.valueOf(getKey()) + ".", io);
			return null;
		}
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		return mFilePropertiesProvider.getRefreshedProperty(name);
	}
	
	/*
	 * Get the duration of the file in milliseconds
	 */
	public int getDuration() throws IOException {
		String durationToParse = mFilePropertiesProvider.getProperty(FilePropertiesProvider.DURATION);
		if (durationToParse != null && !durationToParse.isEmpty())
			return (int) (Double.parseDouble(durationToParse) * 1000);
		throw new IOException("Duration was not present in the song properties.");
	}
}
