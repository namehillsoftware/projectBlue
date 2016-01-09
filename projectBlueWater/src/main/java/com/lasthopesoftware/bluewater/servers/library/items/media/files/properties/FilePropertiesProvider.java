package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties;

import android.util.LruCache;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.vedsoft.fluent.FluentTask;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
        private Integer revision = -1;
        private final ConcurrentSkipListMap<String, String> properties = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

        public void updateProperties(Integer revision, SortedMap<String, String> properties) {
            this.revision = revision;
            this.properties.putAll(properties);
        }

        public Integer getRevision() {
            return revision;
        }

        public ConcurrentSkipListMap<String, String> getProperties() {
            return properties;
        }
    }

	private static final int maxSize = 500;
	private final String fileKeyString;
	private final FilePropertiesContainer filePropertiesContainer;
	private final ConnectionProvider connectionProvider;
	
	private static final ExecutorService filePropertiesExecutor = Executors.newSingleThreadExecutor();
	private static final LruCache<Integer, FilePropertiesContainer> propertiesCache = new LruCache<>(maxSize);
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FilePropertiesProvider.class);

	public FilePropertiesProvider(ConnectionProvider connectionProvider, int fileKey) {
		this.connectionProvider = connectionProvider;
		fileKeyString = String.valueOf(fileKey);


		synchronized (propertiesCache) {
			FilePropertiesContainer cachedFilePropertiesContainer = propertiesCache.get(fileKey);
			if (cachedFilePropertiesContainer == null) {
				cachedFilePropertiesContainer = new FilePropertiesContainer();
				propertiesCache.put(fileKey, cachedFilePropertiesContainer);
			}

			filePropertiesContainer = cachedFilePropertiesContainer;
		}
	}

	public void setProperty(final String name, final String value) {
		if (filePropertiesContainer.getProperties().containsKey(name) && filePropertiesContainer.getProperties().get(name).equals(value)) return;

		filePropertiesExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					final HttpURLConnection conn = connectionProvider.getConnection("File/SetInfo", "File=" + fileKeyString, "Field=" + name, "Value=" + value);
					try {
						conn.setReadTimeout(5000);
						conn.getInputStream().close();
					} finally {
						conn.disconnect();
					}
				} catch (Exception ignored) {
				}
			}
		});

        filePropertiesContainer.getProperties().put(name, value);
	}

	public String getProperty(String name) throws IOException {
		
		if (!filePropertiesContainer.getProperties().containsKey(name))
			return getRefreshedProperty(name);
		
		return filePropertiesContainer.getProperties().get(name);
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		return getRefreshedProperties().get(name);
	}
	
	SortedMap<String, String> getProperties() throws IOException {
		if (filePropertiesContainer.getProperties().size() == 0)
			return getRefreshedProperties();
		
		return Collections.unmodifiableSortedMap(filePropertiesContainer.getProperties());
	}

	SortedMap<String, String> getRefreshedProperties() throws IOException {
	
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		try {
			final SortedMap<String, String> filePropertiesResult = new FluentTask<String, Void, SortedMap<String, String>>() {
				
				@Override
				public SortedMap<String, String> executeInBackground(String... params) {
					final Integer revision = RevisionChecker.getRevision(connectionProvider);
					if (filePropertiesContainer.getProperties().size() > 0 && revision.equals(filePropertiesContainer.getRevision()))
						return Collections.unmodifiableSortedMap(filePropertiesContainer.getProperties());

					final TreeMap<String, String> returnProperties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

					// Seed with old properties first
					returnProperties.putAll(filePropertiesContainer.getProperties());
					
					try {
						final HttpURLConnection conn = connectionProvider.getConnection("File/GetInfo", "File=" + fileKeyString);
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
					} catch (IOException | XmlParseException e) {
						LoggerFactory.getLogger(FilePropertiesProvider.class).error(e.toString(), e);
					}

					filePropertiesContainer.updateProperties(revision, returnProperties);

					return filePropertiesContainer.getProperties();
				}
			}.get(filePropertiesExecutor);

			return Collections.unmodifiableSortedMap(filePropertiesResult);
		} catch (ExecutionException ee) {
			if (ee.getCause() instanceof IOException)
				throw new IOException(ee.getCause());
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
		
		return Collections.unmodifiableSortedMap(filePropertiesContainer.getProperties());
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
	static final String LAST_SKIPPED = "Last Skipped";
	static final String DATE_CREATED = "Date Created";
	static final String DATE_IMPORTED = "Date Imported";
	static final String DATE_MODIFIED = "Date Modified";
	static final String FILE_SIZE = "File Size";
	public static final String AUDIO_ANALYSIS_INFO = "Audio Analysis Info";
	public static final String GET_COVER_ART_INFO = "Get Cover Art Info";
	public static final String IMAGE_FILE = "Image File";
	public static final String KEY = "Key";
	public static final String STACK_FILES = "Stack Files";
	public static final String STACK_TOP = "Stack Top";
	public static final String STACK_VIEW = "Stack View";
	static final String DATE = "Date";
	public static final String RATING = "Rating";
}
