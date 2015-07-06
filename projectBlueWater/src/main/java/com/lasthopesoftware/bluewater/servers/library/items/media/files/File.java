package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

public class File extends AbstractIntKeyStringValue implements IFile {
	private FilePropertiesProvider mFilePropertiesProvider;

	private static final Logger mLogger = LoggerFactory.getLogger(File.class);


	public static final String FILE_URI_SCHEME = "file";
	private static final String MEDIA_DATA_QUERY = 	MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%' ";

	private static final String[] MEDIA_QUERY_PROJECTION = { MediaStore.Audio.Media.DATA };

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
	
	public String getSubItemUrl() {
		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.; 
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */
		return ConnectionProvider.getFormattedUrl("File/GetFile", "File=" + Integer.toString(getKey()), "Quality=medium", "Conversion=Android", "Playback=0");
	}
	
	public void setProperty(String name, String value) {
		mFilePropertiesProvider.setProperty(name, value);
	}
	
	public String getProperty(String name) throws IOException {
		return mFilePropertiesProvider.getProperty(name);
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

	@SuppressLint("InlinedApi")
	@Override
	public Uri getLocalFileUri(Context context) throws IOException {
		if (context == null)
			throw new NullPointerException("The file player's context cannot be null");

		final String originalFilename = getProperty(FilePropertiesProvider.FILENAME);
		if (originalFilename == null)
			throw new IOException("The filename property was not retrieved. A connection needs to be re-established.");

		final String filename = originalFilename.substring(originalFilename.lastIndexOf('\\') + 1, originalFilename.lastIndexOf('.'));

		final StringBuilder querySb = new StringBuilder(MEDIA_DATA_QUERY);
		appendAnd(querySb);

		final ArrayList<String> params = new ArrayList<>(5);
		params.add(filename);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ARTIST, getProperty(FilePropertiesProvider.ARTIST));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ALBUM, getProperty(FilePropertiesProvider.ALBUM));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TITLE, getProperty(FilePropertiesProvider.NAME));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TRACK, getProperty(FilePropertiesProvider.TRACK));

		final Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MEDIA_QUERY_PROJECTION, querySb.toString(), params.toArray(new String[params.size()]), null);
		try {
			if (cursor.moveToFirst()) {
				final String fileUriString = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
				if (fileUriString != null && !fileUriString.isEmpty()) {
					// The file object will produce a properly escaped File URI, as opposed to what is stored in the DB
					final java.io.File file = new java.io.File(fileUriString.replaceFirst(FILE_URI_SCHEME + "://", ""));

					if (file != null && file.exists()) {
						mLogger.info("Returning file URI from local disk.");
						return Uri.fromFile(file);
					}
				}
			}
		} catch (IllegalArgumentException ie) {
			mLogger.info("Illegal column name.", ie);
		} finally {
			cursor.close();
		}

		return null;
	}

	@Override
	public Uri getRemoteFileUri(Context context) throws IOException {
		mLogger.info("Returning file URL from server.");

		final String itemUrl = getSubItemUrl();
		if (itemUrl != null && !itemUrl.isEmpty())
			return Uri.parse(itemUrl);

		return null;
	}

	private static StringBuilder appendPropertyFilter(final StringBuilder querySb, final ArrayList<String> params, final String key, final String value) {
		querySb.append(' ').append(key).append(' ');

		if (value != null) {
			querySb.append(" = ? ");
			params.add(value);
		} else {
			querySb.append(" IS NULL ");
		}

		return querySb;
	}

	private static StringBuilder appendAnd(final StringBuilder querySb) {
		return querySb.append(" AND ");
	}
}
