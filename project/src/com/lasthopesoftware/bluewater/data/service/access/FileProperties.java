package com.lasthopesoftware.bluewater.data.service.access;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;
import android.os.AsyncTask;
import android.util.Log;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class FileProperties {
	private static final int maxSize = 4000;
	private int mFileKey;
	private ConcurrentSkipListMap<String, String> mProperties = null;
	
	private static final ExecutorService filePropertiesExecutor = Executors.newSingleThreadExecutor();
	private static final ConcurrentLinkedHashMap<Integer, ConcurrentSkipListMap<String, String>> mPropertiesCache = new ConcurrentLinkedHashMap.Builder<Integer, ConcurrentSkipListMap<String,String>>().maximumWeightedCapacity(maxSize).build();
	
	public FileProperties(int fileKey) {
		
		mFileKey = fileKey;
		
		if (mPropertiesCache.containsKey(mFileKey)) mProperties = mPropertiesCache.get(mFileKey);
		
		if (mProperties == null) {
			mProperties = new ConcurrentSkipListMap<String, String>(String.CASE_INSENSITIVE_ORDER); 
			mPropertiesCache.put(mFileKey, mProperties);
		}
	}
	
	public void setProperty(String name, String value) {
		if (mProperties.containsKey(name) && mProperties.get(name).equals(value)) return;

		AsyncTask<String, Void, Boolean> setPropertyTask = new AsyncTask<String, Void, Boolean>() {
			
			@Override
			protected Boolean doInBackground(String... params) {
				HttpURLConnection conn = null;
				try {
					conn = ConnectionManager.getConnection("File/SetInfo", "File=" + params[0], "Field=" + params[1], "Value=" + params[2]);
					conn.setReadTimeout(5000);
					conn.getInputStream();
					return true;
				} catch (Exception e) {
					return false;
				} finally {
					if (conn != null)
						conn.disconnect();
				}
			}
		};
		setPropertyTask.executeOnExecutor(filePropertiesExecutor, String.valueOf(mFileKey), name, value);
		
		mProperties.put(name, value);
	}
	

	public String getProperty(String name) throws IOException {
		
		if (mProperties.size() == 0 || !mProperties.containsKey(name))
			return getRefreshedProperty(name);
		
		return mProperties.get(name);
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		return getRefreshedProperties().get(name);
	}

	public String getParsedProperty(final String name) throws IOException {
		return getFormattedValue(name, getProperty(name));
	}
	
	public String getRefreshedFormattedProperty(final String name) throws IOException {
		return getFormattedValue(name, getRefreshedProperty(name));
	}
	
	public SortedMap<String, String> getProperties() throws IOException {
		if (mProperties.size() == 0)
			return getRefreshedProperties();
		
		return Collections.unmodifiableSortedMap(mProperties);
	}
	
	public SortedMap<String, String> getFormattedProperties() throws IOException {
		return buildFormattedReadonlyProperties(getProperties());
	}
	
	public SortedMap<String, String> getRefreshedFormattedProperties() throws IOException {
		return buildFormattedReadonlyProperties(getRefreshedProperties());
	}
		
	public SortedMap<String, String> getRefreshedProperties() throws IOException {
		SortedMap<String, String> result = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		final SimpleTask<String, Void, SortedMap<String,String>> filePropertiesTask = new SimpleTask<String, Void, SortedMap<String,String>>();
		filePropertiesTask.setOnExecuteListener(new OnExecuteListener<String, Void, SortedMap<String,String>>() {
			
			@Override
			public SortedMap<String, String> onExecute(ISimpleTask<String, Void, SortedMap<String, String>> owner, String... params) throws IOException {
				TreeMap<String, String> returnProperties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
				
				try {
					final HttpURLConnection conn = ConnectionManager.getConnection("File/GetInfo", "File=" + String.valueOf(mFileKey));
					conn.setReadTimeout(45000);
					try {
				    	final XmlElement xml = Xmlwise.createXml(IOUtils.toString(conn.getInputStream()));
				    	if (xml.size() < 1) return returnProperties;
				    	
				    	for (XmlElement el : xml.get(0))
				    		returnProperties.put(el.getAttribute("Name"), el.getValue());
				    	
				    	return returnProperties;
					} finally {
						conn.disconnect();
					}
				} catch (MalformedURLException e) {
					LoggerFactory.getLogger(FileProperties.class).error(e.toString(), e);
				} catch (XmlParseException e) {
					LoggerFactory.getLogger(FileProperties.class).error(e.toString(), e);
				}
				
				return returnProperties;
			}
		});
		
		filePropertiesTask.addOnErrorListener(new OnErrorListener<String, Void, SortedMap<String,String>>() {
			
			@Override
			public boolean onError(ISimpleTask<String, Void, SortedMap<String, String>> owner, Exception innerException) {
				return !(innerException instanceof IOException);
			}
		});

		try {
			SortedMap<String, String> filePropertiesResult = filePropertiesTask.executeOnExecutor(filePropertiesExecutor).get();
			
			if (filePropertiesTask.getState() == SimpleTaskState.ERROR) {
				for (Exception e : filePropertiesTask.getExceptions()) {
					if (e instanceof IOException) throw (IOException)e;
				}
			}
			
			if (filePropertiesResult == null) return Collections.unmodifiableSortedMap(mProperties);  
			
			result = Collections.unmodifiableSortedMap(filePropertiesResult);
			
			mProperties.putAll(filePropertiesResult);
		} catch (InterruptedException e) {
			Log.d(getClass().toString(), e.getMessage());
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(FileProperties.class).error(e.toString(), e);
		}
		
		return result;
	}
	
	/* Formatted properties helpers */
	
	private SortedMap<String, String> buildFormattedReadonlyProperties(SortedMap<String, String> unformattedProperties) {
		SortedMap<String, String> formattedProperties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		
		for (Entry<String, String> property : unformattedProperties.entrySet())
			formattedProperties.put(property.getKey(), getFormattedValue(property.getKey(), property.getValue()));
		
		return Collections.unmodifiableSortedMap(formattedProperties);
	}
	
	private static String getFormattedValue(final String name, final String value) {
		if (value == null || value.isEmpty()) return null;
		
		if (mDateTimeProperties.contains(name))
			return parseSecondsIntoDateTime(value).toString();
		
		if (FILE_SIZE.equals(name)) {
			final double filesizeBytes = Long.parseLong(value) / 1024 / 1024;
			final String rawFilesizeString = String.valueOf(filesizeBytes);
			final int periodIndex = rawFilesizeString.indexOf('.');
			if (periodIndex > -1)
				return rawFilesizeString.substring(0, periodIndex + 3) + " MB";
			return rawFilesizeString + " MB";
		}
		
		if (DURATION.equals(name)) {
			final Duration duration = Duration.standardSeconds(Long.parseLong(value));
			final Period period = duration.toPeriod();
			final PeriodFormatter minutesAndSeconds = new PeriodFormatterBuilder()
			     .appendMinutes()
			     .appendSeparator(":")
			     .printZeroAlways()
			     .appendSeconds()
			     .toFormatter();
			return period.toString(minutesAndSeconds);
		}
		
		return value;
	}
	
	private static DateTime parseSecondsIntoDateTime(String secondsString) {
		return new DateTime((long)(Double.parseDouble(secondsString) * 1000));
	}
	
	/* Utility string constants */
	public static final String ARTIST = "Artist";
	public static final String ALBUM_ARTIST = "Album Artist";
	public static final String ALBUM = "Album";
	public static final String DURATION = "Duration";
	public static final String NAME = "Name";
	public static final String FILENAME = "Filename";
	public static final String TRACK = "Track #";
	public static final String NUMBER_PLAYS = "Number Plays";
	public static final String LAST_PLAYED = "Last Played";
	public static final String LAST_SKIPPED = "Last Skipped";
	public static final String DATE_CREATED = "Date Created";
	public static final String DATE_IMPORTED = "Date Imported";
	public static final String DATE_MODIFIED = "Date Modified";
	public static final String FILE_SIZE = "File Size";
	
	private static final Set<String> mDateTimeProperties = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			new String[] { LAST_PLAYED, LAST_SKIPPED, DATE_CREATED, DATE_IMPORTED, DATE_MODIFIED })));
	
}
