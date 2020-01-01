package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.resources.scheduling.ScheduleParsingWork;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.HashMap;
import java.util.Map;

public class SessionFilePropertiesProvider implements ProvideFilePropertiesForSession {

	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesContainerRepository filePropertiesContainerProvider;

	public SessionFilePropertiesProvider(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider, ScheduleParsingWork parsingScheduler) {
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

			return new FilePropertiesPromise(connectionProvider, filePropertiesContainerProvider, serviceFile, revision);
		});
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
