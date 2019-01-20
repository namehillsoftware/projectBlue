package com.lasthopesoftware.bluewater.client.stored.library.sync.specs.GivenASetOfStoredItems;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeFileConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.items.conversion.ConvertStoredPlaylistsToStoredItems;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSyncingTheStoredItemsAndAnErrorOccursDownloading {

	private static List<StoredFile> storedFileJobResults = new ArrayList<>();
	private static List<StoredFile> queuedStoredFiles = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final IStoredItemAccess storedItemAccessMock = mock(IStoredItemAccess.class);
		when(storedItemAccessMock.promiseStoredItems())
			.thenReturn(new Promise<>(Collections.singleton(
				new StoredItem(1, 14, StoredItem.ItemType.ITEM))));

		final FileListParameters fileListParameters = FileListParameters.getInstance();

		final IFileProvider mockFileProvider = mock(IFileProvider.class);
		when(mockFileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(14))))
			.thenReturn(new Promise<>(Arrays.asList(
				new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(4),
				new ServiceFile(10))));

		final FakeFileConnectionProvider fakeConnectionProvider = new FakeFileConnectionProvider();
		fakeConnectionProvider.mapResponse(
			(params) -> { throw new IOException(); },
			"File/GetFile",
			"File=2",
			"Quality=medium",
			"Conversion=Android",
			"Playback=0");

		final IFileReadPossibleArbitrator readPossibleArbitrator = mock(IFileReadPossibleArbitrator.class);
		when(readPossibleArbitrator.isFileReadPossible(any())).thenReturn(true);

		final IFileWritePossibleArbitrator writePossibleArbitrator = mock(IFileWritePossibleArbitrator.class);
		when(writePossibleArbitrator.isFileWritePossible(any())).thenReturn(true);

		final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.pruneStoredFiles(any(), anySet())).thenReturn(Promise.empty());

		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			new Library(),
			new StoredItemServiceFileCollector(storedItemAccessMock, mock(ConvertStoredPlaylistsToStoredItems.class), mockFileProvider),
			storedFileAccess,
			(l, f) -> new Promise<>(new StoredFile(l, 1, f, "fake-file-name", true)),
			new StoredFileJobProcessor(
				new StoredFileSystemFileProducer(),
				storedFileAccess,
				f -> new Promise<>(new IOException()),
				readPossibleArbitrator,
				writePossibleArbitrator,
				(i, f) -> {})
		);

		librarySyncHandler.observeLibrarySync()
			.filter(j -> j.storedFileJobState == StoredFileJobState.Downloaded)
			.map(j -> j.storedFile)
			.blockingSubscribe(new Observer<StoredFile>() {
				@Override
				public void onSubscribe(Disposable d) {

				}

				@Override
				public void onNext(StoredFile storedFile) {
					storedFileJobResults.add(storedFile);
				}

				@Override
				public void onError(Throwable e) {

				}

				@Override
				public void onComplete() {

				}
			});
	}

	@Test
	public void thenTheOtherFilesInTheStoredItemsAreSynced() {
		assertThat(Stream.of(storedFileJobResults).map(StoredFile::getServiceId).toList())
			.containsExactly(1, 4, 10);
	}
}
