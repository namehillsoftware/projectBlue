package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries.AndMultipleErrorsOccur;

import com.annimon.stream.Stream;
import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.resources.FakeMessageSender;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;

import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileDownloadedEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileDownloadingEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileQueuedEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileWriteErrorEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onSyncStopEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.storedFileEventKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSynchronizing extends AndroidContext {

	private static final Random random = new Random();

	private static final StoredFile[] storedFiles = new StoredFile[] {
		new StoredFile().setId(random.nextInt()).setServiceId(1).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(2).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(4).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(5).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(7).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(114).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(92).setLibraryId(4)
	};

	private static final List<Integer> faultingStoredFileServiceIds = Arrays.asList(7, 92);

	private static final List<StoredFile> expectedStoredFileJobs = Stream.of(storedFiles).filter(f -> !faultingStoredFileServiceIds.contains(f.getServiceId())).toList();

	private static final FakeMessageSender fakeMessageSender = new FakeMessageSender();

	@Override
	public void before() {
		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getAllLibraries())
			.thenReturn(new Promise<>(Collections.singletonList(new Library().setId(4))));

		final ControlLibrarySyncs librarySyncHandler = mock(ControlLibrarySyncs.class);
		when(librarySyncHandler.observeLibrarySync(new LibraryId(4)))
			.thenReturn(Observable.concatArrayDelayError(
				Observable
					.fromArray(storedFiles)
					.filter(f -> f.getServiceId() == 92)
					.flatMap(f ->
						Observable.concat(Observable.just(
							new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Queued),
							new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Downloading)),
							Observable.error(new StoredFileReadException(mock(File.class), f))), true),
				Observable
					.fromArray(storedFiles)
					.filter(f -> !faultingStoredFileServiceIds.contains(f.getServiceId()))
					.flatMap(f -> Observable.just(
						new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Queued),
						new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Downloading),
						new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Downloaded))),
				Observable
					.fromArray(storedFiles)
					.filter(f -> f.getServiceId() == 7)
					.flatMap(f ->
						Observable.concat(Observable.just(
							new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Queued),
							new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Downloading)),
							Observable.error(new StoredFileWriteException(mock(File.class), f))), true)));

		final StoredFileSynchronization synchronization = new StoredFileSynchronization(
			libraryProvider,
			fakeMessageSender,
			librarySyncHandler);

		synchronization.streamFileSynchronization().blockingAwait();
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsQueued() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onFileQueuedEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).isSubsetOf(Stream.of(storedFiles).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsDownloading() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onFileDownloadingEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).isSubsetOf(Stream.of(storedFiles).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheWriteErrorsIsBroadcast() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onFileWriteErrorEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsExactlyElementsOf(Stream.of(storedFiles).filter(f -> f.getServiceId() == 7).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheReadErrorsIsBroadcast() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onFileWriteErrorEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsExactlyElementsOf(Stream.of(storedFiles).filter(f -> f.getServiceId() == 7).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onFileDownloadedEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).isSubsetOf(Stream.of(expectedStoredFileJobs).map(StoredFile::getId).toList());
	}

	@Test
	public void thenASyncStoppedEventOccurs() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onSyncStopEvent.equals(i.getAction()))
			.single()).isNotNull();
	}
}
