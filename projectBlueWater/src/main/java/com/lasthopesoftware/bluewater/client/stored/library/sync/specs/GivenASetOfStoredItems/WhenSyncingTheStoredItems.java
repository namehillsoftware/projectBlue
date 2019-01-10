package com.lasthopesoftware.bluewater.client.stored.library.sync.specs.GivenASetOfStoredItems;

import com.annimon.stream.Stream;
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
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSyncingTheStoredItems {

	private static List<StoredFile> storedFileJobResults = new ArrayList<>();
	private static List<StoredFile> queuedStoredFiles = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final IStoredItemAccess storedItemAccessMock = mock(IStoredItemAccess.class);
		when(storedItemAccessMock.promiseStoredItems())
			.thenReturn(new Promise<>(Collections.singleton(
				new StoredItem(1, 14, StoredItem.ItemType.PLAYLIST))));

		final ConvertStoredPlaylistsToStoredItems storedPlaylistsConverter = mock(ConvertStoredPlaylistsToStoredItems.class);
		when(storedPlaylistsConverter.promiseConvertedStoredItem(argThat(a -> a.getServiceId() == 14 && a.getItemType() == StoredItem.ItemType.PLAYLIST)))
			.thenReturn(new Promise<>(new StoredItem(0, 17, StoredItem.ItemType.ITEM)));

		final FileListParameters fileListParameters = FileListParameters.getInstance();

		final IFileProvider mockFileProvider = mock(IFileProvider.class);
		when(mockFileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(17))))
			.thenReturn(new Promise<>(Arrays.asList(
				new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(4),
				new ServiceFile(10))));

		final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.pruneStoredFiles(any(), anySet())).thenReturn(Promise.empty());

		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			new Library(),
			new StoredItemServiceFileCollector(storedItemAccessMock, storedPlaylistsConverter, mockFileProvider),
			storedFileAccess,
			(l, sf) -> new Promise<>(new StoredFile(l, 1, sf, "fake-file-name", true)),
			job -> Observable.just(
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloading),
				new StoredFileJobStatus(
					mock(File.class),
					job.getStoredFile(),
					StoredFileJobState.Downloaded)),
			f -> false,
			f -> false);

		librarySyncHandler.setOnFileQueued(queuedStoredFiles::add);
		storedFileJobResults = librarySyncHandler.observeLibrarySync()
			.filter(j -> j.storedFileJobState == StoredFileJobState.Downloaded)
			.map(j -> j.storedFile)
			.toList()
			.blockingGet();
	}

	@Test
	public void thenTheFilesInTheStoredItemsAreQueued() {
		assertThat(Stream.of(queuedStoredFiles).map(StoredFile::getServiceId).toList())
			.containsExactly(1, 2, 4, 10);
	}

	@Test
	public void thenTheFilesInTheStoredItemsAreSynced() {
		assertThat(Stream.of(storedFileJobResults).map(StoredFile::getServiceId).toList())
			.containsExactly(1, 2, 4, 10);
	}
}
