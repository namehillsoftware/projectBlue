package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties;

import android.util.LruCache;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.vedsoft.fluent.FluentTask;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

public class FilePropertiesProvider extends FluentTask<Integer, Void, Map<String, String>> {

	private static class FilePropertiesContainer {
        private Integer revision = -1;
        private final ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();

        public void updateProperties(Integer revision, Map<String, String> properties) {
            this.revision = revision;
            this.properties.putAll(properties);
        }

        public Integer getRevision() {
            return revision;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

	private static final int maxSize = 500;
	private final String fileKeyString;
	private final FilePropertiesContainer filePropertiesContainer;
	private final IConnectionProvider connectionProvider;
	
	private static final ExecutorService filePropertiesExecutor = Executors.newSingleThreadExecutor();
	private static final LruCache<UrlKeyHolder<Integer>, FilePropertiesContainer> propertiesCache = new LruCache<>(maxSize);

	public FilePropertiesProvider(IConnectionProvider connectionProvider, int fileKey) {
		super(filePropertiesExecutor);

		this.connectionProvider = connectionProvider;
		fileKeyString = String.valueOf(fileKey);

		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);

		synchronized (propertiesCache) {
			FilePropertiesContainer cachedFilePropertiesContainer = propertiesCache.get(urlKeyHolder);
			if (cachedFilePropertiesContainer == null) {
				cachedFilePropertiesContainer = new FilePropertiesContainer();
				propertiesCache.put(urlKeyHolder, cachedFilePropertiesContainer);
			}

			filePropertiesContainer = cachedFilePropertiesContainer;
		}
	}

	@Override
	protected Map<String, String> executeInBackground(Integer[] params) {
		final Integer revision = RevisionChecker.getRevision(connectionProvider);
		if (filePropertiesContainer.getProperties().size() > 0 && revision.equals(filePropertiesContainer.getRevision()))
			return new HashMap<>(filePropertiesContainer.getProperties());

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
			setException(e);
		}

		filePropertiesContainer.updateProperties(revision, returnProperties);

		return new HashMap<>(filePropertiesContainer.getProperties());
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
