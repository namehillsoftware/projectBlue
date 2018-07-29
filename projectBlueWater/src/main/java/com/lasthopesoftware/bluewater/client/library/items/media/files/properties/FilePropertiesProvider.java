package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

public class FilePropertiesProvider implements IFilePropertiesProvider {

	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesContainerRepository filePropertiesContainerProvider;

	private static final ExecutorService filePropertiesExecutor = Executors.newSingleThreadExecutor();


	public FilePropertiesProvider(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesContainerProvider = filePropertiesContainerProvider;
	}

	@Override
	public Promise<Map<String, String>> promiseFileProperties(ServiceFile serviceFile) {
		return RevisionChecker.promiseRevision(connectionProvider).eventually(revision -> {
			final UrlKeyHolder<ServiceFile> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile);
			final FilePropertiesContainer filePropertiesContainer = filePropertiesContainerProvider.getFilePropertiesContainer(urlKeyHolder);
			if (filePropertiesContainer != null && filePropertiesContainer.getProperties().size() > 0 && revision.equals(filePropertiesContainer.revision)) {
				return new Promise<>(new HashMap<>(filePropertiesContainer.getProperties()));
			}

			return new QueuedPromise<>(new FilePropertiesWriter(connectionProvider, filePropertiesContainerProvider, serviceFile, revision), filePropertiesExecutor);
		});
	}

	private static final class FilePropertiesWriter implements CancellableMessageWriter<Map<String, String>> {

		private final IConnectionProvider connectionProvider;
		private final ServiceFile serviceFile;
		private final Integer serverRevision;
		private final IFilePropertiesContainerRepository filePropertiesContainerProvider;

		private FilePropertiesWriter(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider, ServiceFile serviceFile, Integer serverRevision) {
			this.connectionProvider = connectionProvider;
			this.serviceFile = serviceFile;
			this.serverRevision = serverRevision;
			this.filePropertiesContainerProvider = filePropertiesContainerProvider;
		}

		@Override
		public Map<String, String> prepareMessage(CancellationToken cancellationToken) throws Throwable {
			if (cancellationToken.isCancelled())
				throw new CancellationException();

			try {
				if (cancellationToken.isCancelled())
					throw new CancellationException();

				final HttpURLConnection conn = connectionProvider.getConnection("File/GetInfo", "File=" + serviceFile.getKey());
				conn.setReadTimeout(45000);
				try {
					if (cancellationToken.isCancelled())
						throw new CancellationException();

					try (InputStream is = conn.getInputStream()) {
						if (cancellationToken.isCancelled())
							throw new CancellationException();

						final XmlElement xml = Xmlwise.createXml(IOUtils.toString(is));
						final XmlElement parent = xml.get(0);

						final HashMap<String, String> returnProperties = new HashMap<>(parent.size());
						for (XmlElement el : parent)
							returnProperties.put(el.getAttribute("Name"), el.getValue());

						final UrlKeyHolder<ServiceFile> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile);
						filePropertiesContainerProvider.putFilePropertiesContainer(urlKeyHolder, new FilePropertiesContainer(serverRevision, returnProperties));

						return returnProperties;
					}
				} finally {
					conn.disconnect();
				}
			} catch (IOException | XmlParseException e) {
				LoggerFactory.getLogger(FilePropertiesProvider.class).error(e.toString(), e);
				throw e;
			}
		}
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
	static final String DATE_TAGGED = "Date Tagged";
	static final String DATE_FIRST_RATED = "Date First Rated";
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
