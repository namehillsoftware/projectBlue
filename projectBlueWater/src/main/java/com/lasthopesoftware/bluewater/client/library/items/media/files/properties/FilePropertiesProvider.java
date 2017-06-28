package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.Messenger;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;

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
	public Promise<Map<String, String>> promiseFileProperties(int fileKey) {
		return new QueuedPromise<>(new FilePropertiesTask(connectionProvider, filePropertiesContainerProvider, fileKey), filePropertiesExecutor);
	}

	private static final class FilePropertiesTask implements OneParameterAction<Messenger<Map<String, String>>> {

		private final IConnectionProvider connectionProvider;
		private final IFilePropertiesContainerRepository filePropertiesContainerProvider;
		private final Integer fileKey;
		private volatile boolean isCancelled;

		private FilePropertiesTask(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider, Integer fileKey) {
			this.connectionProvider = connectionProvider;
			this.filePropertiesContainerProvider = filePropertiesContainerProvider;
			this.fileKey = fileKey;
		}

		@Override
		public void runWith(Messenger<Map<String, String>> messenger) {
			messenger.cancellationRequested(() -> {
				isCancelled = true;
				messenger.sendRejection(new CancellationException());
			});

			if (isCancelled) return;

			final Integer revision = RevisionChecker.getRevision(connectionProvider);
			final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);

			if (isCancelled) return;

			final FilePropertiesContainer filePropertiesContainer = filePropertiesContainerProvider.getFilePropertiesContainer(urlKeyHolder);
			if (filePropertiesContainer != null && filePropertiesContainer.getProperties().size() > 0 && revision.equals(filePropertiesContainer.revision)) {
				messenger.sendResolution(new HashMap<>(filePropertiesContainer.getProperties()));
				return;
			}

			try {
				if (isCancelled) return;

				final HttpURLConnection conn = connectionProvider.getConnection("File/GetInfo", "File=" + fileKey);
				conn.setReadTimeout(45000);
				try {
					if (isCancelled) return;

					try (InputStream is = conn.getInputStream()) {
						if (isCancelled) return;

						final XmlElement xml = Xmlwise.createXml(IOUtils.toString(is));
						final XmlElement parent = xml.get(0);

						final HashMap<String, String> returnProperties = new HashMap<>(parent.size());
						for (XmlElement el : parent)
							returnProperties.put(el.getAttribute("Name"), el.getValue());

						FilePropertyCache.getInstance().putFilePropertiesContainer(urlKeyHolder, new FilePropertiesContainer(revision, returnProperties));

						messenger.sendResolution(returnProperties);
					}
				} finally {
					conn.disconnect();
				}
			} catch (IOException | XmlParseException e) {
				LoggerFactory.getLogger(FilePropertiesProvider.class).error(e.toString(), e);
				messenger.sendRejection(e);
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
	static final String FILE_SIZE = "ServiceFile Size";
	public static final String AUDIO_ANALYSIS_INFO = "Audio Analysis Info";
	public static final String GET_COVER_ART_INFO = "Get Cover Art Info";
	public static final String IMAGE_FILE = "Image ServiceFile";
	public static final String KEY = "Key";
	public static final String STACK_FILES = "Stack Files";
	public static final String STACK_TOP = "Stack Top";
	public static final String STACK_VIEW = "Stack View";
	static final String DATE = "Date";
	public static final String RATING = "Rating";
	public static final String VolumeLevelR128 = "Volume Level (R128)";
}
