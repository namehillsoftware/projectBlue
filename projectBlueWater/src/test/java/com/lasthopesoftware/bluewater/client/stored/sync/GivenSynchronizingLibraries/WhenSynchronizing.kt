package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries;

import androidx.test.core.app.ApplicationProvider;

import com.annimon.stream.Stream;
import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.resources.FakeMessageBus;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

import io.reactivex.Observable;

import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileDownloadedEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileDownloadingEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileQueuedEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onSyncStartEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onSyncStopEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.storedFileEventKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSynchronizing extends AndroidContext {

	private static final Random random = new Random();

	private static final StoredFile[] storedFiles = new StoredFile[] {
		new StoredFile().setId(random.nextInt()).setServiceId(1).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(2).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(4).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(5).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(114).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(92).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
		new StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
		new StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
		new StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
		new StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
		new StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10)
	};

	private static final FakeMessageBus fakeMessageSender = new FakeMessageBus(ApplicationProvider.getApplicationContext());

	@Override
	public void before() {
		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getAllLibraries())
			.thenReturn(new Promise<>(Arrays.asList(
				new Library().setId(4),
				new Library().setId(10))));

		final ControlLibrarySyncs librarySyncHandler = mock(ControlLibrarySyncs.class);
		when(librarySyncHandler.observeLibrarySync(any()))
			.thenAnswer(a -> Observable
				.fromArray(storedFiles)
				.filter(f -> f.getLibraryId() == a.<LibraryId>getArgument(0).getId())
				.flatMap(f -> Observable.just(
					new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Queued),
					new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Downloading),
					new StoredFileJobStatus(mock(File.class), f, StoredFileJobState.Downloaded))));

		final StoredFileSynchronization synchronization = new StoredFileSynchronization(
			libraryProvider,
			fakeMessageSender,
			librarySyncHandler);

		synchronization.streamFileSynchronization().blockingAwait();
	}

	@Test
	public void thenASyncStartedEventOccurs() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onSyncStartEvent.equals(i.getAction()))
			.single()).isNotNull();
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsQueued() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onFileQueuedEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsExactlyElementsOf(Stream.of(storedFiles).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsDownloading() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onFileDownloadingEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsExactlyElementsOf(Stream.of(storedFiles).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onFileDownloadedEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsExactlyElementsOf(Stream.of(storedFiles).map(StoredFile::getId).toList());
	}

	@Test
	public void thenASyncStoppedEventOccurs() {
		assertThat(Stream.of(fakeMessageSender.getRecordedIntents())
			.filter(i -> onSyncStopEvent.equals(i.getAction()))
			.single()).isNotNull();
	}
}
