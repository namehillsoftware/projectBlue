package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.vedsoft.fluent.FluentSpecifiedTask;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

public class FilePropertiesProvider extends FluentSpecifiedTask<Integer, Void, Map<String, String>> {

	private final int fileKey;
	private final IConnectionProvider connectionProvider;
	
	private static final ExecutorService filePropertiesExecutor = Executors.newSingleThreadExecutor();


	public FilePropertiesProvider(IConnectionProvider connectionProvider, int fileKey) {
		super(filePropertiesExecutor);

		this.connectionProvider = connectionProvider;
		this.fileKey = fileKey;
	}

	@Override
	protected Map<String, String> executeInBackground(Integer[] params) {
		if (isCancelled()) return new HashMap<>();

		final Integer revision = RevisionChecker.getRevision(connectionProvider);
		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);

		if (isCancelled()) return new HashMap<>();

		final FilePropertyCache.FilePropertiesContainer filePropertiesContainer = FilePropertyCache.getInstance().getFilePropertiesContainer(urlKeyHolder);
		if (filePropertiesContainer != null && filePropertiesContainer.getProperties().size() > 0 && revision.equals(filePropertiesContainer.revision))
			return new HashMap<>(filePropertiesContainer.getProperties());

		try {
			if (isCancelled()) return new HashMap<>();

			final HttpURLConnection conn = connectionProvider.getConnection("File/GetInfo", "File=" + fileKey);
			conn.setReadTimeout(45000);
			try {
				if (isCancelled()) return new HashMap<>();

				try (InputStream is = conn.getInputStream()) {
					if (isCancelled()) return new HashMap<>();

					final XmlElement xml = Xmlwise.createXml(IOUtils.toString(is));
					final XmlElement parent = xml.get(0);

					final HashMap<String, String> returnProperties = new HashMap<>(parent.size());
					for (XmlElement el : parent)
						returnProperties.put(el.getAttribute("Name"), el.getValue());

					FilePropertyCache.getInstance().putFilePropertiesContainer(urlKeyHolder, new FilePropertyCache.FilePropertiesContainer(revision, returnProperties));

					return returnProperties;
				}
			} finally {
				conn.disconnect();
			}
		} catch (IOException | XmlParseException e) {
			LoggerFactory.getLogger(FilePropertiesProvider.class).error(e.toString(), e);
			setException(e);
		}

		return new HashMap<>();
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
	public static final String VolumeLevelR128 = "Volume Level (R128)";
}
