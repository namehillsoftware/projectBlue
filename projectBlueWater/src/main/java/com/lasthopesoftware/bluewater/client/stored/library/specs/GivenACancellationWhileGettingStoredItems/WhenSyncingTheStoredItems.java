package com.lasthopesoftware.bluewater.client.stored.library.specs.GivenACancellationWhileGettingStoredItems;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.stored.library.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.items.conversion.ConvertStoredPlaylistsToStoredItems;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.specs.FakeDeferredStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.specs.FakeFileConnectionProvider;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

public class WhenSyncingTheStoredItems {

	private static List<StoredFile> storedFileJobResults = new ArrayList<>();
	private static IStoredFileAccess storedFileAccess;

	@BeforeClass
	public static void before() {
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

		final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(
			job -> Observable.just(
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloading),
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloaded)));

		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			new Library(),
			new StoredItemServiceFileCollector(deferredStoredItemAccess, mock(ConvertStoredPlaylistsToStoredItems.class), mockFileProvider),
			storedFileAccess,
			(l, f) -> new Promise<>(new StoredFile(l, 1, f, "fake-file-name", true)),
			job -> Observable.just(
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloading),
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloaded)),
			mock(ILibraryStorageReadPermissionsRequirementsProvider.class),
			mock(ILibraryStorageWritePermissionsRequirementsProvider.class));

		final Single<List<StoredFile>> syncedFiles = librarySyncHandler.observeLibrarySync().map(j -> j.storedFile).toList();

		librarySyncHandler.cancel();

		deferredStoredItemAccess.resolveStoredItems();

		storedFileJobResults = syncedFiles.blockingGet();
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
