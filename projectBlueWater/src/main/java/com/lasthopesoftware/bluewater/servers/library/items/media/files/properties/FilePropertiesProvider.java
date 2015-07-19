package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties;

import android.util.Log;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

public class FilePropertiesProvider {
    private static class FilePropertiesContainer {
        private Integer mRevision = -1;
        private ConcurrentSkipListMap<String, String> mProperties = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

        public void updateProperties(Integer revision, SortedMap<String, String> properties) {
            mRevision = revision;
            mProperties.putAll(properties);
        }

        public Integer getRevision() {
            return mRevision;
        }

        public ConcurrentSkipListMap<String, String> getProperties() {
            return mProperties;
        }
    }

	private static final int maxSize = 500;
	private final String mFileKeyString;
	private FilePropertiesContainer mFilePropertiesContainer = null;
	
	private static final ExecutorService filePropertiesExecutor = Executors.newSingleThreadExecutor();
	private static final ConcurrentLinkedHashMap<Integer, FilePropertiesContainer> mPropertiesCache = new ConcurrentLinkedHashMap.Builder<Integer, FilePropertiesContainer>().maximumWeightedCapacity(maxSize).build();

	public FilePropertiesProvider(int fileKey) {
		
		mFileKeyString = String.valueOf(fileKey);
		
		if (mPropertiesCache.containsKey(fileKey))
            mFilePropertiesContainer = mPropertiesCache.get(fileKey);

		if (mFilePropertiesContainer == null) {
            mFilePropertiesContainer = new FilePropertiesContainer();
			mPropertiesCache.put(fileKey, mFilePropertiesContainer);
		}
	}

	public void setProperty(final String name, final String value) {
		if (mFilePropertiesContainer.getProperties().containsKey(name) && mFilePropertiesContainer.getProperties().get(name).equals(value)) return;

		filePropertiesExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					final HttpURLConnection conn = ConnectionProvider.getConnection("File/SetInfo", "File=" + mFileKeyString, "Field=" + name, "Value=" + value);;
					try {
						conn.setReadTimeout(5000);
						conn.getInputStream().close();
					} finally {
						conn.disconnect();
					}
				} catch (Exception e) {
					return;
				} 
			}
		});

        mFilePropertiesContainer.getProperties().put(name, value);
	}

	public String getProperty(String name) throws IOException {
		
		if (!mFilePropertiesContainer.getProperties().containsKey(name))
			return getRefreshedProperty(name);
		
		return mFilePropertiesContainer.getProperties().get(name);
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		return getRefreshedProperties().get(name);
	}
	
	public SortedMap<String, String> getProperties() throws IOException {
		if (mFilePropertiesContainer.getProperties().size() == 0)
			return getRefreshedProperties();
		
		return Collections.unmodifiableSortedMap(mFilePropertiesContainer.getProperties());
	}

	public SortedMap<String, String> getRefreshedProperties() throws IOException {
	
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		try {
			final SortedMap<String, String> filePropertiesResult = SimpleTask.executeNew(filePropertiesExecutor, new OnExecuteListener<String, Void, SortedMap<String,String>>() {
				
				@Override
				public SortedMap<String, String> onExecute(ISimpleTask<String, Void, SortedMap<String, String>> owner, String... params) throws IOException {
                    final Integer revision = RevisionChecker.getRevision();
                    if (mFilePropertiesContainer.getProperties().size() > 0 && revision.equals(mFilePropertiesContainer.getRevision()))
                        return Collections.unmodifiableSortedMap(mFilePropertiesContainer.getProperties());

                    final TreeMap<String, String> returnProperties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

                    // Seed with old properties first
				    returnProperties.putAll(mFilePropertiesContainer.getProperties());
					
					try {
						final HttpURLConnection conn = ConnectionProvider.getConnection("File/GetInfo", "File=" + mFileKeyString);
						conn.setReadTimeout(45000);
						try {
							final InputStream is = conn.getInputStream();
							try {
								final XmlElement xml = Xmlwise.createXml(IOUtils.toString(is));
														    	
						    	for (XmlElement el : xml.get(0))
						    		returnProperties.put(el.getAttribute("Name"), el.getValue());
							} finally {
								is.close();
							}
						} finally {
							conn.disconnect();
						}
					} catch (MalformedURLException | XmlParseException e) {
						LoggerFactory.getLogger(FilePropertiesProvider.class).error(e.toString(), e);
					}

					if (returnProperties != null)
                        mFilePropertiesContainer.updateProperties(revision, returnProperties);

                    return mFilePropertiesContainer.getProperties();
				}
			}).get();

			return Collections.unmodifiableSortedMap(filePropertiesResult);
		} catch (ExecutionException ee) {
			if (ee.getCause() instanceof IOException)
				throw new IOException(ee.getCause());
		} catch (InterruptedException e) {
			Log.d(getClass().toString(), e.getMessage());
		} catch (Exception e) {
			LoggerFactory.getLogger(FilePropertiesProvider.class).error(e.toString(), e);
		}
		
		return Collections.unmodifiableSortedMap(mFilePropertiesContainer.getProperties());
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
	public static final String AUDIO_ANALYSIS_INFO = "Audio Analysis Info";
	public static final String GET_COVER_ART_INFO = "Get Cover Art Info";
	public static final String IMAGE_FILE = "Image File";
	public static final String KEY = "Key";
	public static final String STACK_FILES = "Stack Files";
	public static final String STACK_TOP = "Stack Top";
	public static final String STACK_VIEW = "Stack View";
	public static final String DATE = "Date";
	public static final String RATING = "Rating";
}
