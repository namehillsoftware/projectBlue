package com.lasthopesoftware.bluewater.client.library.sync.specs.GivenACancellationWhileGettingStoredItems;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.library.items.stored.specs.FakeDeferredStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.library.sync.specs.FakeFileConnectionProvider;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

public class WhenSyncingTheStoredItems {

	private static List<StoredFile> storedFileJobResults = new ArrayList<>();
	private static IStoredFileAccess storedFileAccess;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeDeferredStoredItemAccess deferredStoredItemAccess = new FakeDeferredStoredItemAccess() {
			@Override
			protected Collection<StoredItem> getStoredItems() {
				return Collections.singleton(new StoredItem(1, 14, StoredItem.ItemType.PLAYLIST));
			}
		};

		final IFileProvider mockFileProvider = mock(IFileProvider.class);
		when(mockFileProvider.promiseFiles(FileListParameters.Options.None, "Playlist/Files", "Playlist=14"))
			.thenReturn(new Promise<>(Arrays.asList(
				new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(4),
				new ServiceFile(10))));

		final FakeFileConnectionProvider fakeConnectionProvider = new FakeFileConnectionProvider();

		final IFileReadPossibleArbitrator readPossibleArbitrator = mock(IFileReadPossibleArbitrator.class);
		when(readPossibleArbitrator.isFileReadPossible(any())).thenReturn(true);

		final IFileWritePossibleArbitrator writePossibleArbitrator = mock(IFileWritePossibleArbitrator.class);
		when(writePossibleArbitrator.isFileWritePossible(any())).thenReturn(true);

		storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.pruneStoredFiles(any(), anySet())).thenReturn(Promise.empty());

		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			new Library(),
			new StoredItemServiceFileCollector(deferredStoredItemAccess, mockFileProvider),
			storedFileAccess,
			(l, f) -> new Promise<>(new StoredFile(l, 1, f, "fake-file-name", true)),
			new StoredFileDownloader(
				new StoredFileSystemFileProducer(),
				fakeConnectionProvider,
				storedFileAccess,
				new ServiceFileUriQueryParamsProvider(),
				readPossibleArbitrator,
				writePossibleArbitrator,
				(i, f) -> {}),
			mock(ILibraryStorageReadPermissionsRequirementsProvider.class),
			mock(ILibraryStorageWritePermissionsRequirementsProvider.class));

		librarySyncHandler.setOnFileDownloaded(jobResult -> storedFileJobResults.add(jobResult.storedFile));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		librarySyncHandler.setOnQueueProcessingCompleted(handler -> countDownLatch.countDown());

		librarySyncHandler.startSync();

		librarySyncHandler.cancel();

		deferredStoredItemAccess.resolveStoredItems();

		countDownLatch.await();
	}

	@Test
	public void thenTheFilesInTheStoredItemsAreNotSynced() {
		assertThat(storedFileJobResults).isEmpty();
	}

	@Test
	public void thenFilesAreNotPruned() {
		verify(storedFileAccess, never()).pruneStoredFiles(any(), anySet());
	}
}
