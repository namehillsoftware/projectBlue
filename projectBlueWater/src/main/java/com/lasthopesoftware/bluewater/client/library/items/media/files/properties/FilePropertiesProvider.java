package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.resources.scheduling.ScheduleParsingWork;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;
import okhttp3.ResponseBody;
import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

public class FilePropertiesProvider implements IFilePropertiesProvider {

	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesContainerRepository filePropertiesContainerProvider;
	private final ScheduleParsingWork parsingScheduler;

	public FilePropertiesProvider(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider, ScheduleParsingWork parsingScheduler) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesContainerProvider = filePropertiesContainerProvider;
		this.parsingScheduler = parsingScheduler;
	}

	@Override
	public Promise<Map<String, String>> promiseFileProperties(ServiceFile serviceFile) {
		return RevisionChecker.promiseRevision(connectionProvider).eventually(revision -> {
			final UrlKeyHolder<ServiceFile> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile);
			final FilePropertiesContainer filePropertiesContainer = filePropertiesContainerProvider.getFilePropertiesContainer(urlKeyHolder);
			if (filePropertiesContainer != null && filePropertiesContainer.getProperties().size() > 0 && revision.equals(filePropertiesContainer.revision)) {
				return new Promise<>(new HashMap<>(filePropertiesContainer.getProperties()));
			}

			return new FilePropertiesPromise(parsingScheduler, connectionProvider, filePropertiesContainerProvider, serviceFile, revision);
		});
	}

	private static final class FilePropertiesPromise extends Promise<Map<String, String>> {

		private FilePropertiesPromise(ScheduleParsingWork parsingScheduler, IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider, ServiceFile serviceFile, Integer serverRevision) {

			final CancellationProxy cancellationProxy = new CancellationProxy();
			respondToCancellation(cancellationProxy);

			final Promise<Response> filePropertiesResponse = connectionProvider.promiseResponse("File/GetInfo", "File=" + serviceFile.getKey());
			cancellationProxy.doCancel(filePropertiesResponse);

			Promise<Map<String, String>> promisedProperties = filePropertiesResponse
				.eventually(new FilePropertiesWriter(parsingScheduler, connectionProvider, filePropertiesContainerProvider, serviceFile, serverRevision));

			cancellationProxy.doCancel(promisedProperties);

			promisedProperties.then(new VoidResponse<>(this::resolve), new VoidResponse<>(this::reject));
		}
	}

	private static final class FilePropertiesWriter implements PromisedResponse<Response, Map<String, String>>, CancellableMessageWriter<Map<String, String>> {

		private final ScheduleParsingWork parsingScheduler;
		private final IConnectionProvider connectionProvider;
		private final ServiceFile serviceFile;
		private final Integer serverRevision;
		private final IFilePropertiesContainerRepository filePropertiesContainerProvider;

		private Response response;

		private FilePropertiesWriter(ScheduleParsingWork parsingScheduler, IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider, ServiceFile serviceFile, Integer serverRevision) {
			this.parsingScheduler = parsingScheduler;
			this.connectionProvider = connectionProvider;
			this.serviceFile = serviceFile;
			this.serverRevision = serverRevision;
			this.filePropertiesContainerProvider = filePropertiesContainerProvider;
		}

		@Override
		public Map<String, String> prepareMessage(CancellationToken cancellationToken) throws Throwable {
			if (cancellationToken.isCancelled()) return new HashMap<>();

			final ResponseBody body = response.body();
			if (body == null) return new HashMap<>();

			try {
				final XmlElement xml = Xmlwise.createXml(body.string());
				final XmlElement parent = xml.get(0);

				final HashMap<String, String> returnProperties = new HashMap<>(parent.size());
				for (final XmlElement el : parent)
					returnProperties.put(el.getAttribute("Name"), el.getValue());

				final UrlKeyHolder<ServiceFile> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile);
				filePropertiesContainerProvider.putFilePropertiesContainer(urlKeyHolder, new FilePropertiesContainer(serverRevision, returnProperties));

				return returnProperties;
			} catch (IOException | XmlParseException e) {
				LoggerFactory.getLogger(FilePropertiesProvider.class).error(e.toString(), e);
				throw e;
			} finally {
				body.close();
			}
		}

		@Override
		public Promise<Map<String, String>> promiseResponse(Response response) {
			this.response = response;
			return new QueuedPromise<>(this, parsingScheduler.getScheduler());
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
	static final String DATE_LAST_OPENED = "Date Last Opened";
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
