package com.lasthopesoftware.bluewater.client.stored.sync.specs.GivenSynchronizingLibraries.AndAStoredFileReadErrorOccurs;

import android.content.Context;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.resources.specs.BroadcastRecorder;
import com.lasthopesoftware.resources.specs.ScopedLocalBroadcastManagerBuilder;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;

import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileDownloadedEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileDownloadingEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileQueuedEvent;
import static com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization.onFileReadErrorEvent;
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
		new StoredFile().setId(random.nextInt()).setServiceId(7).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(114).setLibraryId(4),
		new StoredFile().setId(random.nextInt()).setServiceId(92).setLibraryId(4)
	};

	private static final List<StoredFile> expectedStoredFileJobs = Stream.of(storedFiles).filter(f -> f.getServiceId() != 7).toList();

	private static final BroadcastRecorder broadcastRecorder = new BroadcastRecorder();

	@Override
	public void before() throws Exception {
		final Context context = ApplicationProvider.getApplicationContext();
		final LocalBroadcastManager localBroadcastManager = ScopedLocalBroadcastManagerBuilder.newScopedBroadcastManager(
			context);

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getAllLibraries())
			.thenReturn(new Promise<>(Collections.singletonList(new Library().setId(4))));

		final LibrarySyncHandler librarySyncHandler = mock(LibrarySyncHandler.class);
		when(librarySyncHandler.observeLibrarySync(any()))
			.thenReturn(Observable.concat(
				Observable
					.fromArray(storedFiles)
					.filter(f -> f.getServiceId() != 7)
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
							Observable.error(new StoredFileReadException(mock(File.class), f))), true)));

		final StoredFileSynchronization synchronization = new StoredFileSynchronization(
			libraryProvider,
			localBroadcastManager,
			librarySyncHandler);

		final IntentFilter intentFilter = new IntentFilter(onFileDownloadedEvent);
		intentFilter.addAction(onFileDownloadingEvent);
		intentFilter.addAction(onFileQueuedEvent);
		intentFilter.addAction(onFileReadErrorEvent);

		localBroadcastManager.registerReceiver(
			broadcastRecorder,
			intentFilter);

		synchronization.streamFileSynchronization().blockingAwait();
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsQueued() {
		assertThat(Stream.of(broadcastRecorder.recordedIntents)
			.filter(i -> onFileQueuedEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsOnlyElementsOf(Stream.of(storedFiles).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsDownloading() {
		assertThat(Stream.of(broadcastRecorder.recordedIntents)
			.filter(i -> onFileDownloadingEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsOnlyElementsOf(Stream.of(storedFiles).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheWriteErrorsIsBroadcast() {
		assertThat(Stream.of(broadcastRecorder.recordedIntents)
			.filter(i -> onFileReadErrorEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsExactlyElementsOf(Stream.of(storedFiles).filter(f -> f.getServiceId() == 7).map(StoredFile::getId).toList());
	}

	@Test
	public void thenTheStoredFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(broadcastRecorder.recordedIntents)
			.filter(i -> onFileDownloadedEvent.equals(i.getAction()))
			.map(i -> i.getIntExtra(storedFileEventKey, -1))
			.toList()).containsOnlyElementsOf(Stream.of(expectedStoredFileJobs).map(StoredFile::getId).toList());
	}
}
