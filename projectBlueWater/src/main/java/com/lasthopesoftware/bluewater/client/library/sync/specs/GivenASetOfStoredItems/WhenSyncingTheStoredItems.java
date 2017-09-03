package com.lasthopesoftware.bluewater.client.library.sync.specs.GivenASetOfStoredItems;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.IStoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSyncingTheStoredItems {

	private static List<StoredFile> storedFileJobResults = new ArrayList<>();

	@BeforeClass
	public static void before() throws InterruptedException {
		final IStoredItemAccess storedItemAccessMock = mock(IStoredItemAccess.class);
		when(storedItemAccessMock.promiseStoredItems())
			.thenReturn(new Promise<>(Collections.singleton(generateStoredItem(3, 14))));

		final IFileProvider mockFileProvider = mock(IFileProvider.class);
		when(mockFileProvider.promiseFiles(FileListParameters.Options.None, "Playlist/Files", "Playlist=14" ))
			.thenReturn(new Promise<>(Arrays.asList(
				new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(4),
				new ServiceFile(10))));

		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			mock(IConnectionProvider.class),
			new Library(),
			storedItemAccessMock,
			mock(IStoredFileAccess.class),
			mock(IStoredFileDownloader.class),
			mock(IFileProvider.class),
			mock(ILibraryStorageReadPermissionsRequirementsProvider.class),
			mock(ILibraryStorageWritePermissionsRequirementsProvider.class));

		librarySyncHandler.setOnFileDownloaded(jobResult -> storedFileJobResults.add(jobResult.storedFile));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		librarySyncHandler.setOnQueueProcessingCompleted(handler -> countDownLatch.countDown());

		countDownLatch.await();
	}

	public void thenTheFilesNotInTheStoredItemsAreDeleted() {

	}

	@Test
	public void thenTheFilesInTheStoredItemsAreSynced() {
		assertThat(Stream.of(storedFileJobResults).collect(Collectors.toList())).containsExactly(new StoredFile());
	}

	private static StoredItem generateStoredItem(int storedItemId, int serviceId) {
		final StoredItem storedItem = new StoredItem();
		storedItem.setId(storedItemId);
		storedItem.setServiceId(serviceId);
		storedItem.setItemType(StoredItem.ItemType.PLAYLIST);
		return storedItem;
	}
}
