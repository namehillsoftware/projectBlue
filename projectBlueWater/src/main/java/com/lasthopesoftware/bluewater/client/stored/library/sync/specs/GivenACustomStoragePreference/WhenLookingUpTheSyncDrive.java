package com.lasthopesoftware.bluewater.client.stored.library.sync.specs.GivenACustomStoragePreference;

import com.lasthopesoftware.bluewater.client.library.access.specs.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.storage.directories.specs.FakePrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.specs.FakePublicDirectoryLookup;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenLookingUpTheSyncDrive {

	private static File file;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakePublicDirectoryLookup publicDrives = new FakePublicDirectoryLookup();
		publicDrives.addDirectory("", 1);
		publicDrives.addDirectory("", 2);
		publicDrives.addDirectory("", 3);
		publicDrives.addDirectory("/storage/0/my-big-sd-card", 4);

		final FakePrivateDirectoryLookup fakePrivateDirectoryLookup = new FakePrivateDirectoryLookup();
		fakePrivateDirectoryLookup.addDirectory("fake-private-path", 3);
		fakePrivateDirectoryLookup.addDirectory("/fake-private-path", 5);

		final FakeLibraryProvider fakeLibraryProvider = new FakeLibraryProvider(Collections.singletonList(new Library()
			.setId(14)
			.setSyncedFileLocation(Library.SyncedFileLocation.CUSTOM)
			.setCustomSyncedFilesPath("/stoarage2/my-second-card")));

		final SyncDirectoryLookup syncDirectoryLookup = new SyncDirectoryLookup(
			fakeLibraryProvider,
			publicDrives,
			fakePrivateDirectoryLookup);

		file = new FuturePromise<>(syncDirectoryLookup.promiseSyncDirectory(new LibraryId(14))).get();
	}

	@Test
	public void thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file.getPath()).isEqualTo("/stoarage2/my-second-card");
	}
}
