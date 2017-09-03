package com.lasthopesoftware.bluewater.client.library.sync.specs.GivenASetOfStoredItems;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.IStoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.sync.LibrarySyncHandler;

import org.junit.BeforeClass;

import static org.mockito.Mockito.mock;

public class WhenSyncingTheStoredItems {

	@BeforeClass
	public static void before() {
		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			mock(IConnectionProvider.class),
			new Library(),
			mock(IStoredItemAccess.class),
			mock(IStoredFileAccess.class),
			mock(IStoredFileDownloader.class),
			mock(IFileProvider.class),
			mock(ILibraryStorageReadPermissionsRequirementsProvider.class),
			mock(ILibraryStorageWritePermissionsRequirementsProvider.class));
	}

	public void thenTheFilesNotInTheStoredItemsAreDeleted() {

	}

	public void thenTheFilesInTheStoredItemsAreSynced() {

	}
}
